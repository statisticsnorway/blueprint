package no.ssb.dapla.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.webserver.*;
import no.ssb.dapla.blueprint.notebook.Dependency;
import no.ssb.dapla.blueprint.notebook.Notebook;
import no.ssb.dapla.blueprint.notebook.Repository;
import no.ssb.dapla.blueprint.notebook.Revision;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.helidon.common.http.MediaType.APPLICATION_JSON;
import static org.neo4j.driver.Values.parameters;

public class BlueprintService implements Service {

    static final MediaType APPLICATION_REPOSITORY_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.repository+json");

    static final MediaType APPLICATION_NOTEBOOK_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.notebook+json");

    static final MediaType APPLICATION_REVISION_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.revision+json");

    static final MediaType APPLICATION_DAG_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.dag+json");


    private static final Logger LOG = LoggerFactory.getLogger(BlueprintService.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Driver driver;

    BlueprintService(Config config, Driver driver) {
        this.driver = driver;
    }

    private static RequestPredicate.ConditionalHandler withAccept(Handler handler, MediaType... mediaTypes) {
        return RequestPredicate.create()
                .accepts(mediaTypes)
                .thenApply(handler);
    }

    private static Handler withAcceptAndJson(Handler handler, MediaType mediaType) {
        return withAccept(handler, APPLICATION_JSON, mediaType)
                .otherwise((req, res) -> {
                    throw new HttpException("invalid content-type", Http.Status.NOT_ACCEPTABLE_406);
                });
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/repository",
                        withAcceptAndJson(this::getRepositoriesHandler, APPLICATION_REPOSITORY_JSON))
                .get("/repository/{repoID}/revisions", withAcceptAndJson(this::getRevisionsHandler, APPLICATION_REVISION_JSON))

                .get("/revisions/{revID}/notebooks", RequestPredicate.create()
                        .accepts(APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON).thenApply(this::getNotebooksHandler))
                .get("/revisions/{revID}/notebooks", RequestPredicate.create()
                        .accepts(APPLICATION_DAG_JSON).thenApply(this::getNotebooksHandler).otherwise((req, res) -> {
                            throw new HttpException("invalid content-type", Http.Status.NOT_ACCEPTABLE_406);
                        }))

                .get("/revisions/{revID}/notebooks/{notebookID}", RequestPredicate.create()
                        .accepts(APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .thenApply(this::getNotebookHandler)
                        .otherwise((req, res) -> {
                            throw new HttpException("invalid content-type", Http.Status.NOT_ACCEPTABLE_406);
                        })
                )

                .get("/revisions/{revID}/notebooks/{notebookID}/inputs", this::getNotebookInputsHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/outputs", this::getNotebookOutputsHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/previous", this::getPreviousNotebooksHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/next", this::getNextNotebooksHandler)

                .put("/rev/{rev}", this::putRevisionHandler)
                .get("/rev/{rev}", this::getRevisionHandler)
                .delete("/rev/{rev}", this::deleteRevisionHandler)
        ;
    }

    private void getRepositoriesHandler(ServerRequest request, ServerResponse response) {
        response.status(Http.Status.OK_200).send(List.of(
                new Repository("foo"),
                new Repository("bar")
        ));
    }

    private void getRevisionsHandler(ServerRequest request, ServerResponse response) {
        response.status(Http.Status.OK_200).send(List.of(
                new Revision("foo"),
                new Revision("bar")
        ));
    }

    private void getNotebooksHandler(ServerRequest request, ServerResponse response) {
        var nb1 = new Notebook();
        nb1.fileName = "/foo";
        var nb2 = new Notebook();
        nb2.fileName = "/bar";
        var nb3 = new Notebook();
        nb3.fileName = "/foo/bar";
        var nb4 = new Notebook();
        nb4.fileName = "/bar/foo";

        response.status(Http.Status.OK_200).send(List.of(
                nb1, nb2, nb3, nb4
        ));
    }

    private void getNotebooksDAGHandler(ServerRequest request, ServerResponse response) {
        var nb1 = new Notebook();
        nb1.fileName = "/foo";
        var nb2 = new Notebook();
        nb2.fileName = "/bar";
        var nb3 = new Notebook();
        nb3.fileName = "/foo/bar";
        var nb4 = new Notebook();
        nb4.fileName = "/bar/foo";

        response.status(Http.Status.OK_200).send(List.of(
                new Dependency(nb1, nb2),
                new Dependency(nb1, nb3),
                new Dependency(nb2, nb4),
                new Dependency(nb3, nb4),
                new Dependency(nb1, nb4)
        ));
    }

    private void getNotebookHandler(ServerRequest request, ServerResponse response) {
        var nb1 = new Notebook();
        nb1.fileName = "/foo";
        response.status(Http.Status.OK_200).send(nb1);
    }

    private void getNotebookOutputsHandler(ServerRequest request, ServerResponse response) {
        request.next(new UnsupportedOperationException("TODO"));
    }

    private void getNotebookInputsHandler(ServerRequest request, ServerResponse response) {
        request.next(new UnsupportedOperationException("TODO"));
    }

    private void getNextNotebooksHandler(ServerRequest request, ServerResponse response) {
        request.next(new UnsupportedOperationException("TODO"));
    }

    private void getPreviousNotebooksHandler(ServerRequest request, ServerResponse response) {
        request.next(new UnsupportedOperationException("TODO"));
    }

    private void putRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.writeTransaction(tx -> {
                Result result = tx.run("CREATE (r:GitRevision) SET r.revision = $rev RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(201).send(node);
        }
    }

    private void getRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.readTransaction(tx -> {
                Result result = tx.run("MATCH (r:GitRevision {revision: $rev}) RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(200).send(node);
        }
    }

    private void deleteRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            ResultSummary resultSummary = session.writeTransaction(tx -> {
                Result result = tx.run("MATCH (r:GitRevision {revision: $rev}) OPTIONAL MATCH (r)<--(nb) OPTIONAL MATCH (r)<--(nb)--(ds) DETACH DELETE r, nb, ds",
                        parameters("rev", request.path().param("rev")));
                return result.consume();
            });
            int nodesDeleted = resultSummary.counters().nodesDeleted();
            int relationshipsDeleted = resultSummary.counters().relationshipsDeleted();
            response.status(200).send(mapper.createObjectNode()
                    .put("nodesDeleted", nodesDeleted)
                    .put("relationshipsDeleted", relationshipsDeleted));
        }
    }
}
