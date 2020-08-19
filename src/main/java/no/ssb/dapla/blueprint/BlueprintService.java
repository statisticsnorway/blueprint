package no.ssb.dapla.blueprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.blueprint.notebook.Dependency;
import no.ssb.dapla.blueprint.notebook.Notebook;
import no.ssb.dapla.blueprint.notebook.Repository;
import no.ssb.dapla.blueprint.notebook.Revision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static io.helidon.common.http.MediaType.APPLICATION_JSON;

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


    private final NotebookStore store;

    BlueprintService(Config config, NotebookStore store) {
        this.store = Objects.requireNonNull(store);
    }

    private static String getRevision(ServerRequest request) {
        return Objects.requireNonNull(request.path().param("revID"));
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

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/repository", MediaTypeHandler.create()
                        .accept(this::getRepositoriesHandler, APPLICATION_REPOSITORY_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/repository/{repoID}/revisions", MediaTypeHandler.create()
                        .accept(this::getRevisionsHandler, APPLICATION_REVISION_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/revisions/{revID}/notebooks", MediaTypeHandler.create()
                        .accept(this::getNotebooksHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .accept(this::getNotebooksDAGHandler, APPLICATION_DAG_JSON)
                        .orFail()
                )
                .get("/revisions/{revID}/notebooks/{notebookID}", MediaTypeHandler.create()
                        .accept(this::getNotebookHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .orFail()
                )

                .get("/revisions/{revID}/notebooks/{notebookID}/inputs", this::getNotebookInputsHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/outputs", this::getNotebookOutputsHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/previous", this::getPreviousNotebooksHandler)
                .get("/revisions/{revID}/notebooks/{notebookID}/next", this::getNextNotebooksHandler);
    }

    private void getNotebooksHandler(ServerRequest request, ServerResponse response) {
        var revisionId = getRevision(request);
        var notebooks = store.getNotebooks(revisionId);
        response.status(Http.Status.OK_200).send(notebooks);
    }

    private void getNotebooksDAGHandler(ServerRequest request, ServerResponse response) {
        var revisionId = getRevision(request);
        var dependencies = store.getDependencies(revisionId);
        response.status(Http.Status.OK_200).send(dependencies);
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
}
