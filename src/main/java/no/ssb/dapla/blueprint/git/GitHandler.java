package no.ssb.dapla.blueprint.git;


import com.fasterxml.jackson.databind.JsonNode;
import io.helidon.config.Config;
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
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static io.helidon.config.ConfigSources.classpath;

public class GitHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GitHandler.class);
    public static final String HMAC_SHA1 = "HmacSHA1";

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

    public static void handleHook(JsonNode payload, String neo4JUrl) {

//        Config config = Config
//                .builder(classpath("application-dev.yaml"), // TODO how to use env specific config?
//                        classpath("application.yaml"))
//                .metaConfig()
//                .build();
//        Config neo4jConfig = config.get("neo4j");
//
//        String dbUrl = "bolt://" + neo4jConfig.get("host").asString().get() + ":" + neo4jConfig.get("port").asInt().get();

        String repoUrl = payload.get("repository").get("clone_url").textValue();
        try {
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider( // TODO from env vars
                                    "", ""))
                    .call();

            Parser.Options options = new Parser.Options();
            options.commitId = payload.get("after").textValue();
            options.helpRequested = false;
            options.repositoryURL = repoUrl;
            options.host = URI.create(neo4JUrl);
            options.root = new File(payload.get("repository").get("name").textValue());
            Parser.parse(options);

        } catch (GitAPIException e) {
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
