package no.ssb.dapla.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.helidon.config.Config;
import io.helidon.media.common.DefaultMediaSupport;
import io.helidon.media.jackson.common.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BlueprintTest {

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintTest.class);

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    static {
        BlueprintApplication.initLogging();
    }

    private static WebServer webServer;

    @BeforeAll
    public static void startTheServer(Config config) {
        Neo4J.initializeEmbedded(config.get("neo4j"));
        long webServerStart = System.currentTimeMillis();
        webServer = new BlueprintApplication(config).get(WebServer.class);
        webServer.start().toCompletableFuture()
                .thenAccept(webServer -> {
                    long duration = System.currentTimeMillis() - webServerStart;
                    LOG.info("Server started in {} ms, listening at port {}", duration, webServer.port());
                })
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
    }

    @AfterAll
    public static void stopServer() {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .orTimeout(10, TimeUnit.SECONDS)
                    .join();
        }
    }

    @Test
    public void testHelloWorld() {
        WebClient webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(DefaultMediaSupport.create(false))
                .addMediaSupport(JacksonSupport.create(mapper))
                .build();

        webClient.put()
                .path("/blueprint/rev/a21be2b45")
                .submit()
                .thenAccept(response -> Assertions.assertEquals(201, response.status().code()))
                .thenCompose(nothing -> webClient.get()
                        .path("/blueprint/rev/a21be2b45")
                        .request())
                .thenCompose(response -> {
                    Assertions.assertEquals(200, response.status().code());
                    return response.content().as(JsonNode.class);
                })
                .thenAccept(jsonNode -> Assertions.assertEquals("a21be2b45", jsonNode.get("revision").textValue()))
                .thenCompose(nothing -> webClient.delete()
                        .path("/blueprint/rev/a21be2b45")
                        .request())
                .thenCompose(response -> {
                    Assertions.assertEquals(200, response.status().code());
                    return response.content().as(JsonNode.class);
                })
                .thenAccept(jsonNode -> {
                    Assertions.assertEquals(1, jsonNode.get("nodesDeleted").numberValue());
                    Assertions.assertEquals(0, jsonNode.get("relationshipsDeleted").numberValue());
                })
                .exceptionally(throwable -> {
                    Assertions.fail(throwable);
                    return null;
                })
                .toCompletableFuture()
                .orTimeout(60, TimeUnit.SECONDS)
                .join();
    }
}
