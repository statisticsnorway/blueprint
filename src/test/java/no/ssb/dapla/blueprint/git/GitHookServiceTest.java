package no.ssb.dapla.blueprint.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.config.Config;
import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.notebook.Notebook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class GitHookServiceTest {

    private Path tmpDir;
    private String remoteRepoDir;

    private Git remoteGit;
    private JsonNode payload;

    private GitHookService handler;

    @BeforeEach
    void setUp(Config config, Driver driver) throws Exception {

        driver.session().writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));

        handler = new GitHookService(config, new NotebookStore(driver));

        // set up local and fake remote Git repo
        tmpDir = Files.createTempDirectory(null);

        // Copy the test notebook directory from the test/resources/*.ipynb folder to the fake remote Git directory
        String remoteRepoDirName = "remoteRepo";
        remoteRepoDir = String.join(File.separator, tmpDir.toString(), remoteRepoDirName) + File.separator;
        Files.createDirectory(Paths.get(remoteRepoDir));
        copyFiles("/notebooks");

        // Create the fake remote Git repo
        Repository remoteRepo = new FileRepository(remoteRepoDir + ".git");
        remoteRepo.create();

        remoteGit = new Git(remoteRepo);
        payload = commitToRemote("First commit from remote repository");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walkFileTree(tmpDir, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                } else {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            }
        });
    }

    @Test
    void testHook(Driver driver) throws Exception {
        String firstCommitId = payload.get("after").textValue();
        handler.checkoutAndParse(payload);
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();
        assertThat(notebooks.size()).isEqualTo(2);

        // Test new commit
        copyFiles("/notebooks/graph/commit1");
        JsonNode secondPayload = commitToRemote("Second commit from remote repository");
        handler.checkoutAndParse(secondPayload);
        String secondCommitId = secondPayload.get("after").textValue();
        assertThat(secondCommitId).isNotEqualTo(firstCommitId);
        notebooks = notebookStore.getNotebooks();

        // TODO is 7 correct here? We have added three new notebooks, but the two original ones are
        // added to Neo4J on both runs
        assertThat(notebooks.size()).isEqualTo(7);

        // Delete file and commit
        JsonNode deletePayload = deleteFileFromRemote("Delete file", "Freg.ipynb");
        handler.checkoutAndParse(deletePayload);
        assertThat(deletePayload.get("after").textValue()).isNotEqualTo(secondCommitId);
        notebooks = notebookStore.getNotebooks();
        assertThat(notebooks.size()).isEqualTo(11);
    }

    private void copyFiles(String s) throws IOException {
        Path testNotebooks = Paths.get(GitHookServiceTest.class.getResource(s).getPath());
        Path destination = Paths.get(remoteRepoDir);
        Stream<Path> jupyterNotebooks = Files.walk(testNotebooks, 1);
        jupyterNotebooks.forEach(notebook -> {
            if (notebook.toString().endsWith(".ipynb")) {
                try {
                    Files.copy(notebook, destination.resolve(testNotebooks.relativize(notebook)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        jupyterNotebooks.close();
    }

    private JsonNode commitToRemote(String commitMessage) throws GitAPIException {
        remoteGit.add().addFilepattern(".").call();
        RevCommit commitRevision = remoteGit.commit().setMessage(commitMessage).call();

        ObjectNode newPayload = new ObjectMapper().createObjectNode();
        newPayload.put("after", commitRevision.getId().toString());
        ObjectNode repoNode = newPayload.putObject("repository");
        repoNode
                .put("clone_url", remoteRepoDir + ".git")
                .put("name", "remoteRepo");

        return newPayload;
    }

    private JsonNode deleteFileFromRemote(String commitMessage, String filename) throws GitAPIException, IOException {
        Files.delete(Paths.get(remoteRepoDir + File.separator + filename));
        remoteGit.rm().addFilepattern(filename).call();
        RevCommit commitRevision = remoteGit.commit().setMessage(commitMessage).call();
        ObjectNode newPayload = new ObjectMapper().createObjectNode();
        newPayload.put("after", commitRevision.getId().toString());
        ObjectNode repoNode = newPayload.putObject("repository");
        repoNode
                .put("clone_url", remoteRepoDir + ".git")
                .put("name", "remoteRepo");

        return newPayload;
    }
}