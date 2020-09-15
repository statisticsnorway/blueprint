package no.ssb.dapla.blueprint.rest;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.blueprint.neo4j.GitStore;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static io.helidon.common.http.MediaType.APPLICATION_JSON;

public class BlueprintService implements Service {

    static final MediaType APPLICATION_REPOSITORY_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.repository+json");

    static final MediaType APPLICATION_NOTEBOOK_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.notebook+json");

    static final MediaType APPLICATION_JUPYTER_JSON = MediaType.create(
            "application", "x-ipynb+json");

    static final MediaType APPLICATION_REVISION_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.revision+json");

    static final MediaType APPLICATION_DAG_JSON = MediaType.create(
            "application", "vnd.ssb.blueprint.dag+json");

    private final NotebookStore notebookStore;
    private final GitStore gitStore;

    public BlueprintService(NotebookStore notebookStore, GitStore gitStore) {
        this.notebookStore = Objects.requireNonNull(notebookStore);
        this.gitStore = Objects.requireNonNull(gitStore);
    }

    private static String parseCommitId(ServerRequest request) {
        return Objects.requireNonNull(request.path().param("commitId"));
    }

    private static String parseNotebookId(ServerRequest request) {
        return Objects.requireNonNull(request.path().param("notebookId"));
    }

    private static String parseRepositoryId(ServerRequest request) {
        return Objects.requireNonNull(request.path().param("repoId"));
    }

    private void getRepositoriesHandler(ServerRequest request, ServerResponse response) {
        var repositories = notebookStore.getRepositories();
        response.status(Http.Status.OK_200).send(repositories);
    }

    private void getRevisionsHandler(ServerRequest request, ServerResponse response) {
        var revisionId = parseRepositoryId(request);
        Optional<Collection<Commit>> commits = notebookStore.getCommits(revisionId);
        if (commits.isEmpty()) {
            response.status(Http.Status.NOT_FOUND_404).send();
        } else {
            response.status(Http.Status.OK_200).send(commits.get());
        }
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/repositories", MediaTypeHandler.create()
                        .accept(this::getRepositoriesHandler, APPLICATION_REPOSITORY_JSON, APPLICATION_JSON)
                        .orFail()
                )

                .get("/repositories/{repoId}/commits", MediaTypeHandler.create()
                        .accept(this::getRevisionsHandler, APPLICATION_REVISION_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks", MediaTypeHandler.create()
                        .accept(this::getNotebooksHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .accept(this::getNotebooksDAGHandler, APPLICATION_DAG_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks/{notebookId}", MediaTypeHandler.create()
                        .accept(this::getNotebookContentHandler, APPLICATION_JUPYTER_JSON)
                        .accept(this::getNotebookHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .orFail()
                );
    }

    private void getNotebookContentHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var revisionId = parseCommitId(request);
        var notebookId = parseNotebookId(request);

        var notebooks = notebookStore.getNotebook(revisionId, notebookId);
        try {
            byte[] content = gitStore.getBlob(repositoryId, notebooks.getBlobId());
            response.send(content);
        } catch (Exception e) {
            response.send(e);
        }
    }

    private void getNotebooksHandler(ServerRequest request, ServerResponse response) {
        var revisionId = parseCommitId(request);
        var notebooks = notebookStore.getNotebooks(revisionId);
        response.status(Http.Status.OK_200).send(notebooks);
    }

    private void getNotebooksDAGHandler(ServerRequest request, ServerResponse response) {
        var revisionId = parseCommitId(request);
        var dependencies = notebookStore.getDependencies(revisionId);
        response.status(Http.Status.OK_200).send(dependencies);
    }

    private void getNotebookHandler(ServerRequest request, ServerResponse response) {
        var revisionId = parseCommitId(request);
        var notebookId = parseNotebookId(request);
        Notebook notebook = notebookStore.getNotebook(revisionId, notebookId);
        response.status(Http.Status.OK_200).send(notebook);
    }
}
