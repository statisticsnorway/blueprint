package no.ssb.dapla.blueprint.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.Http;
import io.helidon.webserver.*;
import no.ssb.dapla.blueprint.neo4j.GitStore;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import no.ssb.dapla.blueprint.parser.GitNotebookProcessor;
import no.ssb.dapla.blueprint.parser.Neo4jOutput;
import no.ssb.dapla.blueprint.parser.NotebookFileVisitor;
import no.ssb.dapla.blueprint.parser.Parser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

import static io.helidon.common.http.Http.Status.*;

/**
 * A HTTP service that listens to github webhook to fetch and parse new commits.
 * <p>
 * The repository are saved and reused. In order to facilitate addressing, the hash of the
 * repository is used as a key.
 */
public class GithubHookService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(GithubHookService.class);
    private static final Http.ResponseStatus TOO_MANY_REQUESTS = Http.ResponseStatus.create(429, "Too Many Requests");
    private static final String HOOK_PATH = "/githubhook";
    private static final int GITHOOK_TIMEOUT = 10;
    private final GithubHookVerifier verifier;
    private final ExecutorService parserExecutor;
    private final ObjectMapper mapper = new ObjectMapper();
    private final NotebookStore notebookStore;
    private final GitStore gitStore;

    public GithubHookService(NotebookStore notebookStore, GitStore gitStore, GithubHookVerifier verifier) throws NoSuchAlgorithmException {
        this.notebookStore = Objects.requireNonNull(notebookStore);
        this.gitStore = Objects.requireNonNull(gitStore);
        this.verifier = Objects.requireNonNull(verifier);
        this.parserExecutor = Executors.newFixedThreadPool(4);
    }

    public synchronized void checkoutAndParse(JsonNode payload) {

        String repoUrl = payload.get("repository").get("clone_url").textValue();

        try (Git git = Git.wrap(gitStore.get(URI.create(repoUrl)))) {

            var commitId = payload.get("head_commit").get("id").textValue();

            // Checkout head_commit from repo
            git.checkout().setName(commitId).call();

            var path = git.getRepository().getWorkTree().toPath();
            var processor = new GitNotebookProcessor(new ObjectMapper(), git);
            var visitor = new NotebookFileVisitor(Set.of(".git"));
            var output = new Neo4jOutput(notebookStore);
            Parser parser = new Parser(visitor, output, processor);

            var commit = new Commit(commitId);
            var repository = new Repository(repoUrl);
            repository.addCommit(commit);
            commit.setRepository(repository);
            parser.parse(path, commit);

        } catch (GitAPIException e) {
            LOG.error("Error connecting to remote repository", e);
        } catch (IOException e) {
            LOG.error("Error parsing notebooks", e);
        }
    }

    private void postGitPushHook(ServerRequest request, ServerResponse response, byte[] body) {
        try {

            if (!verifier.checkSignature(request, body)) {
                response.status(UNAUTHORIZED_401).send();
                return;
            }

            JsonNode node = mapper.readTree(body);

            // Hooks need to respond within GITHOOK_TIMEOUT seconds so we run it async.
            CompletableFuture.runAsync(() -> checkoutAndParse(node), parserExecutor)
                    .orTimeout(GITHOOK_TIMEOUT, TimeUnit.SECONDS)
                    .handle((parseResult, throwable) -> {
                        if (throwable == null) {
                            response.status(CREATED_201).send();
                        } else if (throwable instanceof TimeoutException) {
                            LOG.warn("Could not parse before timeout. Failures won't be reported");
                            response.status(ACCEPTED_202).send();
                        } else {
                            request.next(throwable);
                        }
                        return null;
                    });

        } catch (RejectedExecutionException ree) {
            response.status(TOO_MANY_REQUESTS).send();
        } catch (Exception e) {
            request.next(e);
        }
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.post(HOOK_PATH, Handler.create(byte[].class, this::postGitPushHook));
    }
}
