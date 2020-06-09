package no.ssb.dapla.blueprint.git;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.*;
import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.parser.Neo4jOutput;
import no.ssb.dapla.blueprint.parser.NotebookFileVisitor;
import no.ssb.dapla.blueprint.parser.Parser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.*;

import static io.helidon.common.http.Http.Status.*;

public class GitHookService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(GitHookService.class);
    private static final Http.ResponseStatus TOO_MANY_REQUESTS = Http.ResponseStatus.create(429, "Too Many Requests");
    private static final String HOOK_PATH = "/githubhook";
    private static final int GITHOOK_TIMEOUT = 10;
    private final Parser parser;
    private final Config config;
    private final GithubHookVerifier verifier;
    private final ExecutorService parserExecutor;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHookService(Config config, NotebookStore store) throws NoSuchAlgorithmException {
        this.parser = new Parser(new NotebookFileVisitor(Set.of()), new Neo4jOutput(store));
        this.config = config;
        this.verifier = new GithubHookVerifier(config.get("github.secret").asString().get());
        // Keeping it simple for now.
        this.parserExecutor = Executors.newFixedThreadPool(4);
    }

    public void checkoutAndParse(JsonNode payload) {

        String repoUrl = payload.get("repository").get("clone_url").textValue();
        try {
            var cloneCall = Git.cloneRepository().setURI(repoUrl);
            if (config.get("github.username").hasValue() && config.get("github.password").hasValue()) {
                cloneCall.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        config.get("github.username").asString().get(),
                        config.get("github.password").asString().get()
                ));
            }
            cloneCall.call();

            var path = Path.of(payload.get("repository").get("name").textValue());
            var commitId = payload.get("after").textValue();

            parser.parse(path, commitId, repoUrl);

        } catch (GitAPIException | IOException e) {
            LOG.error("Error connecting to remote repository", e);
        } finally {
            // delete local repo
            String localRepoPath = payload.get("repository").get("name").textValue();
            try {
                FileUtils.delete(new File(localRepoPath), FileUtils.RECURSIVE);
            } catch (IOException e) {
                LOG.error("Failed to delete repo at {}", localRepoPath, e);
            }
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
            CompletableFuture.runAsync(() -> {
                checkoutAndParse(node);
            }, parserExecutor).orTimeout(GITHOOK_TIMEOUT, TimeUnit.SECONDS).handle((parseResult, throwable) -> {
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
