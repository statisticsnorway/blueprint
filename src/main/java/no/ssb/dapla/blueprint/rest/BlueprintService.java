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
import no.ssb.dapla.blueprint.rest.json.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .get("/repositories/{repoId}/commits/{commitId}", MediaTypeHandler.create()
                        .accept(this::getRevisionHandler, APPLICATION_REVISION_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks", MediaTypeHandler.create()
                        .accept(this::getNotebooksHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .accept(this::getNotebooksAsDAGHandler, APPLICATION_DAG_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks/{notebookId}", MediaTypeHandler.create()
                        .accept(this::getNotebookContentHandler, APPLICATION_JUPYTER_JSON)
                        .accept(this::getNotebookHandler, APPLICATION_NOTEBOOK_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks/{notebookId}/forward", MediaTypeHandler.create()
                        .accept(this::getDagForward, APPLICATION_DAG_JSON, APPLICATION_JSON)
                        .orFail()
                )
                .get("/repositories/{repoId}/commits/{commitId}/notebooks/{notebookId}/backward", MediaTypeHandler.create()
                        .accept(this::getDagBackward, APPLICATION_DAG_JSON, APPLICATION_JSON)
                        .orFail()
                );

    }

    private void getNotebookContentHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var notebookId = parseNotebookId(request);
        var notebook = notebookStore.getNotebook(repositoryId, commitId, notebookId);
        try {
            byte[] content = gitStore.getBlob(repositoryId, notebook.getBlobId());
            response.send(content);
        } catch (Exception e) {
            response.send(e);
        }
    }

    private void getNotebooksHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var commit = notebookStore.getCommit(repositoryId, commitId);
        if (commit.isEmpty()) {
            response.status(Http.Status.NOT_FOUND_404).send();
        } else {
            List<NotebookSummary> summaries = Stream.of(
                    commit.get().getUpdates(),
                    commit.get().getCreates(),
                    commit.get().getUnchanged()
            ).flatMap(Collection::stream)
                    .map(NotebookSummary::new)
                    .collect(Collectors.toList());

            response.status(Http.Status.OK_200).send(summaries);
        }
    }

    private void sendDAG(ServerResponse response, Optional<Commit> commitWithDependencies) {
        if (commitWithDependencies.isEmpty()) {
            response.status(Http.Status.NOT_FOUND_404).send();
        } else {
            Commit commit = commitWithDependencies.get();
            List<NotebookDetail> notebooks = Stream.of(
                    commit.getCreates(),
                    commit.getUpdates(),
                    commit.getUnchanged()
            ).flatMap(Collection::stream).map(NotebookDetail::new).collect(Collectors.toList());
            response.status(Http.Status.OK_200).send(new DirectedAcyclicGraph(notebooks));
        }
    }

    private void getNotebooksAsDAGHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var commitWithDependencies = notebookStore.getDependencies(repositoryId, commitId);
        sendDAG(response, commitWithDependencies);
    }

    private void getDagBackward(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var notebookId = parseNotebookId(request);
        var commitWithDependencies = notebookStore.getBackwardDependencies(repositoryId, commitId, notebookId);
        sendDAG(response, commitWithDependencies);
    }

    private void getDagForward(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var notebookId = parseNotebookId(request);
        var commitWithDependencies = notebookStore.getForwardDependencies(repositoryId, commitId, notebookId);
        sendDAG(response, commitWithDependencies);
    }

    private void getNotebookHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var notebookId = parseNotebookId(request);
        Notebook notebook = notebookStore.getNotebook(repositoryId, commitId, notebookId);
        response.status(Http.Status.OK_200).send(notebook);
    }

    private void getRevisionsHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        Optional<Collection<Commit>> commits = notebookStore.getCommits(repositoryId);
        if (commits.isEmpty()) {
            response.status(Http.Status.NOT_FOUND_404).send();
        } else {
            List<CommitSummary> summaries = commits.get().stream().map(CommitSummary::new).collect(Collectors.toList());
            response.status(Http.Status.OK_200).send(summaries);
        }
    }

    private void getRevisionHandler(ServerRequest request, ServerResponse response) {
        var repositoryId = parseRepositoryId(request);
        var commitId = parseCommitId(request);
        var commit = notebookStore.getCommit(repositoryId, commitId);
        if (commit.isEmpty()) {
            response.status(Http.Status.NOT_FOUND_404).send();
        } else {
            response.status(Http.Status.OK_200).send(new CommitDetail(commit.get()));
        }
    }
}
