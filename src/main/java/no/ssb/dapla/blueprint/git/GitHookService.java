package no.ssb.dapla.blueprint.git;


import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.config.Config;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class GitHookService {

    public static final String HMAC_SHA1 = "HmacSHA1";
    private static final Logger LOG = LoggerFactory.getLogger(GitHookService.class);
    private final Parser parser;
    private final Config config;

    public GitHookService(Config config, NotebookStore store) {
        parser = new Parser(new NotebookFileVisitor(Set.of()), new Neo4jOutput(store));
        this.config = config;
    }

    public static boolean verifySignature(String signature, String secret, String payload) {
        if (signature == null || signature.length() != 45) {
            return false;
        }
        final Mac mac;
        try {
            mac = Mac.getInstance(HMAC_SHA1);
            final SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA1);
            mac.init(signingKey);

            byte[] bytes = mac.doFinal(payload.getBytes());
            char[] hash = new char[2 * bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                hash[2 * i] = "0123456789abcdef".charAt((bytes[i] & 0xf0) >> 4);
                hash[2 * i + 1] = "0123456789abcdef".charAt(bytes[i] & 0x0f);
            }
            final String expected = "sha1=" + String.valueOf(hash);
            return signature.equals(expected);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            // TODO handle
            return false;
        }
    }

    public void handleHook(JsonNode payload) {

        String repoUrl = payload.get("repository").get("clone_url").textValue();
        try {
            var cloneCall = Git.cloneRepository().setURI(repoUrl);
            // TODO: Use Key.
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
}
