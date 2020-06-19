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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class GitHookServiceTest {

    private static final String REMOTE_REPO_BASE_NAME = "remoteRepo_";
    private static final String NOTEBOOK_NAME = "notebook-with-metadata";
    private static final String NOTEBOOK_FILE_EXTENSION = ".ipynb";
    private List<Path> tmpDirList = new ArrayList<>();

    private GitHookService handler;


    Git createRemoteRepo(Config config, Driver driver, int repoCounter) throws Exception {

        driver.session().writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));

        handler = new GitHookService(config, new NotebookStore(driver));

        // set up local and fake remote Git repo
        tmpDirList.add(Files.createTempDirectory(null));

        // Copy the test notebook directory from the test/resources/*.ipynb folder to the fake remote Git directory
        String remoteRepoDirName = REMOTE_REPO_BASE_NAME + repoCounter;
        String remoteRepoDir = String.join(File.separator, tmpDirList.get(repoCounter).toString(), remoteRepoDirName) + File.separator;
        Files.createDirectory(Paths.get(remoteRepoDir));
        copyFiles("/notebooks", remoteRepoDir);

        // Create the fake remote Git repo
        Repository remoteRepo = new FileRepository(remoteRepoDir + ".git");
        remoteRepo.create();

        return new Git(remoteRepo);
    }

    @AfterEach
    void tearDown() throws IOException {
        for (Path tmpDir : tmpDirList) {
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
    }

    @Test
    void testHook(Config config, Driver driver) throws Exception {
        int repoCounter = 0;
        Git remoteRepo = createRemoteRepo(config, driver, repoCounter);
        JsonNode payloadInitialCommit = commitToRemote("Initial commit", remoteRepo, repoCounter);
        String firstCommitId = payloadInitialCommit.get("head_commit").get("id").textValue();
        handler.checkoutAndParse(payloadInitialCommit);
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();
        assertThat(notebooks.size()).isEqualTo(2);

        // Test new commit
        createNewFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoCounter), repoCounter);
        JsonNode secondPayload = commitToRemote("Second commit from remote repository", remoteRepo, repoCounter);
        handler.checkoutAndParse(secondPayload);
        String secondCommitId = secondPayload.get("head_commit").get("id").textValue();
        assertThat(secondCommitId).isNotEqualTo(firstCommitId);
        notebooks = notebookStore.getNotebooks();

        // Two from first commit and three from second
        assertThat(notebooks.size()).isEqualTo(5);

        // Delete file and commit
        JsonNode deletePayload = deleteFileFromRemote("Delete file", NOTEBOOK_NAME + NOTEBOOK_FILE_EXTENSION, remoteRepo, repoCounter);
        handler.checkoutAndParse(deletePayload);
        assertThat(deletePayload.get("head_commit").get("id").textValue()).isNotEqualTo(secondCommitId);
        notebooks = notebookStore.getNotebooks();

        // Two from first commit, three from second and two from third
        assertThat(notebooks.size()).isEqualTo(7);
    }

    @Test
    void testNotLatestCommit(Config config, Driver driver) throws Exception {
        int repoNumber = 0;
        Git remoteRepo = createRemoteRepo(config, driver, repoNumber);
        JsonNode payloadInitialCommit = commitToRemote("Initial commit", remoteRepo, repoNumber);
        String firstCommitId = payloadInitialCommit.get("head_commit").get("id").textValue();

        // Do a second commit, not included in payload
        createNewFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoNumber), repoNumber);
        JsonNode secondPayload = commitToRemote("Second commit from remote repository", remoteRepo, repoNumber);
        assertThat(firstCommitId).isNotEqualTo(secondPayload.get("after").textValue());

        handler.checkoutAndParse(payloadInitialCommit);
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();
        assertThat(notebooks.size()).isEqualTo(2);
    }

    @Test
    void testAsyncSameRepo(Config config, Driver driver) throws Exception {
        int repoCounter = 0;
        Git remoteRepo = createRemoteRepo(config, driver, repoCounter);

        // Do initial commit of files
        commitToRemote("Initial commit", remoteRepo, repoCounter);

        int numberOfThreads = 5;
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfThreads);
        CountDownLatch callingThreadBlocker = new CountDownLatch(1);
        CountDownLatch completedThreadCounter = new CountDownLatch(numberOfThreads);

        // Create commits and payloads
        var payloads = new ArrayList<JsonNode>();
        for (int i = 0; i < numberOfThreads; i++) {
            createNewFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoCounter), i);
            payloads.add(commitToRemote("Commit number " + i, remoteRepo, repoCounter));
        }

        List<Thread> workers = new ArrayList<>();
        int expectedNumberOfNotebooks = 0;
        int initialNumberOfNotebooks = 3;// First commit contains three notebooks
        for (int i = 0; i < numberOfThreads; i++) {
            workers.add(new Thread(new GitHookWorker(
                    readyThreadCounter, callingThreadBlocker, completedThreadCounter, payloads.get(i))));
            expectedNumberOfNotebooks += initialNumberOfNotebooks++;
        }

        workers.forEach(Thread::start);
        readyThreadCounter.await();

        callingThreadBlocker.countDown();
        completedThreadCounter.await();
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();

        assertThat(notebooks.size()).isEqualTo(expectedNumberOfNotebooks);
    }

    @Test
    void testAsyncMultipleRepos(Config config, Driver driver) throws Exception {
        int numberOfThreads = 5;
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfThreads);
        CountDownLatch callingThreadBlocker = new CountDownLatch(1);
        CountDownLatch completedThreadCounter = new CountDownLatch(numberOfThreads);

        // Create repos, commits and payloads
        var payloads = new ArrayList<JsonNode>();
        for (int i = 0; i < numberOfThreads; i++) {
            Git remoteRepo = createRemoteRepo(config, driver, i);
            createNewFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), i), i);
            payloads.add(commitToRemote("Commit number " + 0, remoteRepo, i));
        }

        List<Thread> workers = new ArrayList<>();
        int expectedNumberOfNotebooks = numberOfThreads * 3; // Three files in each repo
        for (int i = 0; i < numberOfThreads; i++) {
            workers.add(new Thread(new GitHookWorker(
                    readyThreadCounter, callingThreadBlocker, completedThreadCounter, payloads.get(i))));
        }

        workers.forEach(Thread::start);
        readyThreadCounter.await();

        callingThreadBlocker.countDown();
        completedThreadCounter.await();
        NotebookStore notebookStore = new NotebookStore(driver);
        List<Notebook> notebooks = notebookStore.getNotebooks();

        assertThat(notebooks.size()).isEqualTo(expectedNumberOfNotebooks);
    }

    private void createNewFileInRepo(String repoDir, int counter) throws IOException {
        Path destinationPath = Paths.get(repoDir);
        Path sourceFile = Paths.get(String.join(File.separator, destinationPath.toString(), NOTEBOOK_NAME + NOTEBOOK_FILE_EXTENSION));
        Path destinationFileName = Paths.get(sourceFile.toString() + "_" + counter + NOTEBOOK_FILE_EXTENSION);
        Files.copy(sourceFile, destinationFileName, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyFiles(String source, String destination) throws IOException {
        Path testNotebooks = Paths.get(GitHookServiceTest.class.getResource(source).getPath());
        Path destinationPath = Paths.get(destination);
        Stream<Path> jupyterNotebooks = Files.walk(testNotebooks, 1);
        jupyterNotebooks.forEach(notebook -> {
            if (notebook.toString().endsWith(".ipynb")) {
                try {
                    Files.copy(notebook, destinationPath.resolve(testNotebooks.relativize(notebook)),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        jupyterNotebooks.close();
    }

    private JsonNode commitToRemote(String commitMessage, Git remoteRepo, int repoCounter) {
        ObjectNode newPayload = new ObjectMapper().createObjectNode();
        try {
            remoteRepo.add().addFilepattern(".").call();
            RevCommit commitRevision = remoteRepo.commit().setMessage(commitMessage).call();

            newPayload.put("after", commitRevision.getName());
            ObjectNode commitNode = newPayload.putObject("head_commit");
            commitNode.put("id", commitRevision.getName());
            ObjectNode repoNode = newPayload.putObject("repository");
            repoNode
                    .put("clone_url", resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoCounter)
                            + File.separator + ".git")
                    .put("name", REMOTE_REPO_BASE_NAME + repoCounter);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return newPayload;
    }

    private JsonNode deleteFileFromRemote(
            String commitMessage,
            String filename,
            Git remoteRepo,
            int repoCounter) throws GitAPIException, IOException {

        String remoteRepoDir = resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoCounter);
        Files.delete(Paths.get(remoteRepoDir + File.separator + filename));
        remoteRepo.rm().addFilepattern(filename).call();
        RevCommit commitRevision = remoteRepo.commit().setMessage(commitMessage).call();
        ObjectNode newPayload = new ObjectMapper().createObjectNode();
        newPayload.put("after", commitRevision.getName());
        ObjectNode commitNode = newPayload.putObject("head_commit");
        commitNode.put("id", commitRevision.getName());
        ObjectNode repoNode = newPayload.putObject("repository");
        repoNode
                .put("clone_url", remoteRepoDir + File.separator + ".git")
                .put("name", REMOTE_REPO_BASE_NAME + repoCounter);

        return newPayload;
    }

    private String resolveRepoDir(String fullGitPath, int repoCounter) {
        String[] dirs = fullGitPath.split("/");
        return String.join(File.separator, tmpDirList.get(repoCounter).toString(), dirs[dirs.length - 2]); // Get the second to last element
    }

    class GitHookWorker implements Runnable {
        private final CountDownLatch readyThreadCounter;
        private final CountDownLatch callingThreadBlocker;
        private final CountDownLatch completedThreadCounter;
        private final JsonNode payload;

        public GitHookWorker(CountDownLatch readyThreadCounter, CountDownLatch callingThreadBlocker,
                             CountDownLatch completedThreadCounter, JsonNode payload) {
            this.readyThreadCounter = readyThreadCounter;
            this.callingThreadBlocker = callingThreadBlocker;
            this.completedThreadCounter = completedThreadCounter;
            this.payload = payload;
        }

        @Override
        public void run() {
            readyThreadCounter.countDown();
            try {
                callingThreadBlocker.await();
                handler.checkoutAndParse(payload);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                completedThreadCounter.countDown();
            }

        }
    }

}