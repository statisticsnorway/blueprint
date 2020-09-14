package no.ssb.dapla.blueprint.rest;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import no.ssb.dapla.blueprint.BlueprintApplication;
import no.ssb.dapla.blueprint.HelidonConfigExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HelidonConfigExtension.class)
class BlueprintServiceTest {

    private static WebServer server;
    private static WebClient client;

    @BeforeAll
    static void beforeAll(Config config) throws InterruptedException, ExecutionException, TimeoutException, NoSuchAlgorithmException {
        server = new BlueprintApplication(config).get(WebServer.class);
        server.start().get(10, TimeUnit.SECONDS);
        client = WebClient.builder().baseUri("http://localhost:" + server.port()).build();
    }

    @AfterAll
    static void afterAll() throws InterruptedException, ExecutionException, TimeoutException {
        server.shutdown().get(10, TimeUnit.SECONDS);
    }

    @Test
    void testListRepository() {
        var response = client.get()
                .path("/api/v1/repository")
                .accept(MediaType.APPLICATION_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.OK_200);
    }

    @Test
    void testListRevision() {
        var response = client.get()
                .path("/api/v1/repository/repoID/revisions")
                .accept(MediaType.APPLICATION_JSON).submit();
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);

        response = client.get()
                .path("/api/v1/repository/repoID/revisions")
                .accept(BlueprintService.APPLICATION_REVISION_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.OK_200);

        response = client.get()
                .path("/api/v1/repository/repoID/revisions")
                .accept(MediaType.TEXT_HTML).submit();
        // See https://github.com/joel-costigliola/assertj-core/issues/1214#issuecomment-675440543
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.NOT_ACCEPTABLE_406);
    }

    @Test
    void testListNotebooks() {
        var response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks")
                .accept(MediaType.APPLICATION_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.OK_200);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks")
                .accept(BlueprintService.APPLICATION_NOTEBOOK_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.OK_200);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks")
                .accept(BlueprintService.APPLICATION_DAG_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.OK_200);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks")
                .accept(MediaType.TEXT_HTML).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.NOT_ACCEPTABLE_406);
    }

    @Test
    void testTodos() {
        var response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks/{notebookID}/inputs").submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.INTERNAL_SERVER_ERROR_500);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks/{notebookID}/outputs").submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.INTERNAL_SERVER_ERROR_500);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks/{notebookID}/previous").submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.INTERNAL_SERVER_ERROR_500);

        response = client.get()
                .path("/api/v1/revisions/{revID}/notebooks/{notebookID}/next").submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.INTERNAL_SERVER_ERROR_500);
    }
}