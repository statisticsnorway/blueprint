package no.ssb.dapla.blueprint.rest;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import no.ssb.dapla.blueprint.BlueprintApplication;
import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Dataset;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static no.ssb.dapla.blueprint.WebClientResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class BlueprintServiceTest {

    private static WebServer server;
    private static WebClient client;
    private static NotebookStore notebookStore;

    @BeforeAll
    static void beforeAll(Config config) throws InterruptedException, ExecutionException, TimeoutException, NoSuchAlgorithmException {
        BlueprintApplication application = new BlueprintApplication(config);
        server = application.get(WebServer.class);
        notebookStore = application.getNotebookStore();
        server.start().get(10, TimeUnit.SECONDS);
        client = WebClient.builder().baseUri("http://localhost:" + server.port()).build();
    }

    @AfterAll
    static void afterAll() throws InterruptedException, ExecutionException, TimeoutException {
        server.shutdown().get(10, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUp() {
        notebookStore.purgeDatabase();
    }

    @Test
    void testListRepositories() {

        var repository = new Repository("http://example.com/foo/bar");
        notebookStore.saveRepository(repository);

        var relativeRepository = new Repository("bar/foo");
        notebookStore.saveRepository(relativeRepository);

        var absoluteRepository = new Repository("/foo/bar");
        notebookStore.saveRepository(absoluteRepository);

        var response = client.get()
                .path("/api/v1/repositories")
                .accept(MediaType.APPLICATION_JSON).submit();

        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);

        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent("""
                        [
                            {
                                "id":"4e9fd323445b023e20fc952bebbd2d340ab5a7dd",
                                "uri":"http://example.com/foo/bar"
                            },{
                                "id" : "ceb1a787ad7f07dd8a289d2c4c34aee239367a66",
                                "uri" : "bar/foo"
                            },{
                                "id" : "a82cce35fd860de6f63f97e6c482dc6a14d002e8",
                                "uri" : "/foo/bar"
                            }
                        ]
                        """);
    }

    @Test
    void testListCommits() {

        var repository = new Repository("foo/bar");
        var commit1 = new Commit("commit1");
        commit1.setAuthorName("Hadrien");
        commit1.setAuthoredAt(Instant.ofEpochMilli(0));
        var commit2 = new Commit("commit2");
        commit2.setAuthorName("Arild");
        commit2.setAuthoredAt(Instant.ofEpochMilli(1000));
        repository.addCommit(commit1);
        repository.addCommit(commit2);

        notebookStore.saveRepository(repository);


        var expectedResponse = """
                [{
                    "id":"commit2",
                    "author":"Arild",
                    "createdAt":1.0
                },{
                    "id":"commit1",
                    "author":"Hadrien",
                    "createdAt":0.0
                }]
                """;

        var response = client.get()
                .path("/api/v1/repositories/" + repository.getId() + "/commits")
                .accept(MediaType.APPLICATION_JSON).submit();

        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent(expectedResponse);

        response = client.get()
                .path("/api/v1/repositories/" + repository.getId() + "/commits")
                .accept(BlueprintService.APPLICATION_REVISION_JSON).submit();
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent(expectedResponse);

        response = client.get()
                .path("/api/v1/repositories/DOESNOTEXIST/commits")
                .accept(BlueprintService.APPLICATION_REVISION_JSON).submit();
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.NOT_FOUND_404);

        response = client.get()
                .path("/api/v1/repositories/" + repository.getId() + "/commits")
                .accept(MediaType.TEXT_HTML).submit();
        // See https://github.com/joel-costigliola/assertj-core/issues/1214#issuecomment-675440543
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.NOT_ACCEPTABLE_406);
    }

    @Test
    void testListNotebooks() {

        var repository = new Repository("foo/bar");
        notebookStore.saveRepository(repository);

        var commit1 = new Commit("commit1");
        commit1.setAuthorName("Hadrien");
        commit1.setAuthoredAt(Instant.ofEpochMilli(0));
        var commit2 = new Commit("commit2");
        commit2.setAuthorName("Arild");
        commit2.setAuthoredAt(Instant.ofEpochMilli(1000));
        repository.addCommit(commit1);
        repository.addCommit(commit2);

        var nb1 = new Notebook("nb1");
        nb1.addInputs(new Dataset("/a"));
        nb1.addInputs(new Dataset("/b"));
        nb1.addOutputs(new Dataset("/c"));
        nb1.addOutputs(new Dataset("/d"));

        var nb2 = new Notebook("nb2");
        nb2.addInputs(new Dataset("/c"));
        nb2.addInputs(new Dataset("/d"));
        nb2.addOutputs(new Dataset("/e"));
        nb2.addOutputs(new Dataset("/f"));

        commit1.addCreate("foo", nb1);
        commit1.addCreate("foo/bar", nb2);

        var nb3 = new Notebook("nb3");
        nb3.addInputs(new Dataset("/e"));
        nb3.addInputs(new Dataset("/b"));
        nb3.addOutputs(new Dataset("/g"));
        commit2.addCreate("bar/foo", nb3);
        commit2.addUpdate("foo", nb1);
        commit2.addUpdate("foo/bar", nb2);

        notebookStore.saveRepository(repository);

        var response = client.get()
                .path("/api/v1/repositories/" + repository.getId() + "/commits/" + commit1.getId() + "/notebooks")
                .accept(MediaType.APPLICATION_JSON).submit();

        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent("""
                        [{
                            "id": "nb2",
                            "commitId": "commit1",
                            "path": "foo/bar",
                            "fetchUrl": "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit1/notebooks/nb2"
                        }, {
                            "id": "nb1",
                            "commitId": "commit1",
                            "path": "foo",
                            "fetchUrl": "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit1/notebooks/nb1"
                        }]
                        """);

        response = client.get()
                .path("/api/v1/repositories/" + repository.getId() + "/commits/" + commit2.getId() + "/notebooks")
                .accept(BlueprintService.APPLICATION_NOTEBOOK_JSON).submit();
        assertThat(response).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent("""
                        [{
                            "id": "nb2",
                            "commitId": "commit2",
                            "path": "foo/bar",
                            "fetchUrl": "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb2"
                        }, {
                            "id": "nb1",
                            "commitId": "commit2",
                            "path": "foo",
                            "fetchUrl": "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb1"
                        }, {
                            "id" : "nb3",
                            "commitId" : "commit2",
                            "path" : "bar/foo",
                            "fetchUrl" : "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb3"
                        }]
                        """);

        response = client.get()
                .path("/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks")
                .accept(BlueprintService.APPLICATION_DAG_JSON).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(response.await())
                .hasStatus(Http.Status.OK_200)
                .hasJsonContent("""
                        {
                          "nodes" : [ {
                            "inputs" : [ "/b", "/e" ],
                            "outputs" : [ "/g" ],
                            "id" : "nb3",
                            "commitId" : "commit2",
                            "path" : "bar/foo",
                            "fetchUrl" : "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb3"
                          }, {
                            "inputs" : [ "/c", "/d" ],
                            "outputs" : [ "/e", "/f" ],
                            "id" : "nb2",
                            "commitId" : "commit2",
                            "path" : "foo/bar",
                            "fetchUrl" : "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb2"
                          }, {
                            "inputs" : [ "/a", "/b" ],
                            "outputs" : [ "/c", "/d" ],
                            "id" : "nb1",
                            "commitId" : "commit2",
                            "path" : "foo",
                            "fetchUrl" : "/api/v1/repositories/17cdeaefa5cc6022481c824e15a47a7726f593dd/commits/commit2/notebooks/nb1"
                          } ],
                          "edges" : [ {
                            "from" : "nb2",
                            "to" : "nb3"
                          }, {
                            "from" : "nb1",
                            "to" : "nb2"
                          } ]
                        }
                        """);

        response = client.get()
                .path("/api/v1/repositories/{repoId}/commits/{commitId}/notebooks")
                .accept(MediaType.TEXT_HTML).submit();
        assertThat(response)
                .succeedsWithin(1, TimeUnit.SECONDS)
                .extracting(WebClientResponse::status)
                .isEqualTo(Http.Status.NOT_ACCEPTABLE_406);
    }

    @Test
    void testListNotebooksDag() {

    }
}