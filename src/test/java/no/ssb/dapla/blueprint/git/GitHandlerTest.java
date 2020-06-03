package no.ssb.dapla.blueprint.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.dapla.blueprint.BaseTestClass;
import no.ssb.dapla.blueprint.Neo4jTestContainer;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(Neo4jTestContainer.class)
class GitHandlerTest extends BaseTestClass {

    private Path tmpDir;
    private String remoteRepoDir;

    private Git remoteGit;
    private JsonNode payload;

    @BeforeEach
    void setUp() throws Exception {

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
    void testHook(Object[] params) throws Exception{
        assertParams(params);
        Driver driver = (Driver) params[0];
        String boltUrl = (String) params[1];
        String firstCommitId = payload.get("after").textValue();
        GitHandler.handleHook(payload, boltUrl);
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();
        assertThat(notebooks.size()).isEqualTo(2);

        // Test new commit
        copyFiles("/notebooks/graph/commit1");
        JsonNode secondPayload = commitToRemote("Second commit from remote repository");
        GitHandler.handleHook(secondPayload, boltUrl);
        assertThat(secondPayload.get("after").textValue()).isNotEqualTo(firstCommitId);
        notebooks = notebookStore.getNotebooks();

        // TODO is 7 correct here? We have added three new notebooks, but the two original ones are
        // added to Neo4J on both runs
        assertThat(notebooks.size()).isEqualTo(7);
    }

    private void copyFiles(String s) throws IOException {
        Path testNotebooks = Paths.get(GitHandlerTest.class.getResource(s).getPath());
        Path destionation = Paths.get(remoteRepoDir);
        Stream<Path> jupyterNotebooks = Files.walk(testNotebooks, 1);
        jupyterNotebooks.forEach(notebook -> {
            if (notebook.toString().endsWith(".ipynb")) {
                try {
                    Files.copy(notebook, destionation.resolve(testNotebooks.relativize(notebook)),
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
        RevCommit firstCommitRevision = remoteGit.commit().setMessage(commitMessage).call();

        ObjectNode newPayload = new ObjectMapper().createObjectNode();
        newPayload.put("after", firstCommitRevision.getId().toString());
        ObjectNode repoNode = newPayload.putObject("repository");
        repoNode
                .put("clone_url", remoteRepoDir + ".git")
                .put("name", "remoteRepo");

        return newPayload;
    }
}