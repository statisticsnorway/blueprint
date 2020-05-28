package no.ssb.dapla.blueprint.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

@ExtendWith(EmbeddedNeo4jExtension.class)
class GitHandlerTest {

    private Path tmpDir;
    private final String remoteRepoDirName = "remoteRepo";
    private final String localRepoDirName = "notebooks";
    private String remoteRepoDir;

    private Git remoteGit;
    private JsonNode payload;


    @BeforeEach
    void setUp() throws Exception {
        // set up local and fake remote Git repo
        tmpDir = Files.createTempDirectory(null);

        // Copy the test notebook directory from the test/resources/*.ipynb folder to the fake remote Git directory
        remoteRepoDir = String.join(File.separator, tmpDir.toString(), remoteRepoDirName) + File.separator;
        Files.createDirectory(Paths.get(remoteRepoDir));
        Path testNotebooks = Paths.get(GitHandlerTest.class.getResource("/notebooks").getPath());
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

        // Create the fake remote Git repo
        Repository remoteRepo = new FileRepository(remoteRepoDir + ".git");
        remoteRepo.create();

        remoteGit = new Git(remoteRepo);
        remoteGit.add().addFilepattern(".").call();
        RevCommit firstCommitRevision = remoteGit.commit().setMessage("First commit from remote repository").call();

        payload = new ObjectMapper().createObjectNode();
        ((ObjectNode) payload).put("after", firstCommitRevision.getId().toString());
        ObjectNode repoNode = ((ObjectNode) payload).putObject("repository");
        repoNode
                .put("clone_url", remoteRepoDir + ".git")
                .put("name", "remoteRepo");
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
    void testHook() {
        GitHandler.handleHook(payload);
        // TODO assert stuff
    }
}