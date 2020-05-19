/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static io.helidon.config.ConfigSources.classpath;

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
    public static void startTheServer() {
        Config config = Config
                .builder(classpath("application-dev.yaml"),
                        classpath("application.yaml"))
                .metaConfig()
                .build();
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
