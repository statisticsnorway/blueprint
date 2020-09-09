package no.ssb.dapla.blueprint.neo4j;

import io.helidon.config.Config;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static no.ssb.dapla.blueprint.neo4j.model.Repository.computeHash;

/**
 * Stores and cache repositories.
 */
public class GitStore {

    private static final Path GIT_FOLDER = Path.of(".git");
    private static final Path LINKS_FOLDER = Path.of("links");

    // TODO: Use a proper cache.
    private final Map<String, Repository> hashRepoMap = new HashMap<>();
    private final Config config;

    public GitStore(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Initialize, clone and fetch a repository.
     */
    public Repository get(URI remote) throws IOException, GitAPIException {
        String hash = computeHash(remote);

        // Initialize.
        if (!hashRepoMap.containsKey(hash)) {
            hashRepoMap.put(hash, initializeRepository(hash, remote));
        }

        Repository repository = hashRepoMap.get(hash);

        // Fetch latest always.
        authenticate(Git.wrap(repository).fetch()).call();

        return repository;
    }

    /**
     * Helper that authenticates git commands.
     */
    private <R, C extends GitCommand<R>> TransportCommand<C, R> authenticate(TransportCommand<C, R> command) {
        if (config.get("github.username").hasValue() && config.get("github.password").hasValue()) {
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    config.get("github.username").asString().get(),
                    config.get("github.password").asString().get()
            ));
        }
        return command;
    }

    private Repository initializeRepository(String hashPath, URI remote) throws IOException, GitAPIException {
        Path repositoryPath = Path.of(config.get("github.path").asString().get());

        // Create the file
        File hashFile = repositoryPath.resolve(hashPath).toFile();
        if (!hashFile.exists()) {
            Files.createDirectories(hashFile.toPath());
        }

        // Create a link for administration. Note the substring to get rid of the first '/'.
        File linkFile = repositoryPath.resolve(LINKS_FOLDER).resolve(remote.getPath().substring(1)).toFile();
        if (!linkFile.exists()) {
            Files.createDirectories(linkFile.toPath().getParent());
            Files.createSymbolicLink(
                    linkFile.toPath(),
                    linkFile.toPath().getParent().relativize(hashFile.toPath())
            );
        }

        // Setup repository only the first time.
        if (!hashFile.toPath().resolve(GIT_FOLDER).toFile().exists()) {
            CloneCommand cloneCall = Git.cloneRepository()
                    .setURI(remote.toASCIIString())
                    .setCloneAllBranches(true)
                    .setDirectory(hashFile);
            return authenticate(cloneCall).call().getRepository();
        } else {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            return builder.setWorkTree(hashFile).setup().build();
        }
    }

}
