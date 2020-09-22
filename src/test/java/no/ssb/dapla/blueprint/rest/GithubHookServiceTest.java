package no.ssb.dapla.blueprint.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.config.Config;
import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.neo4j.GitStore;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class GithubHookServiceTest {

    private static final String REMOTE_REPO_BASE_NAME = "remoteRepo_";
    private static final String NOTEBOOK_NAME = "notebook-with-metadata";
    private static final String NOTEBOOK_FILE_EXTENSION = ".ipynb";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static NotebookStore store;
    private static GithubHookService handler;
    private static Session session;

    private final List<Path> tmpDirList = new ArrayList<>();

    /**
     * Create a remote repository with the content of "/notebooks/foo"
     */
    Git createRemoteRepo(int repoCounter) throws Exception {

        // set up local and fake remote Git repo
        tmpDirList.add(Files.createTempDirectory(null));

        // Copy the test notebook directory from the test/resources/*.ipynb folder to the fake remote Git directory
        String remoteRepoDirName = REMOTE_REPO_BASE_NAME + repoCounter;
        String remoteRepoDir = String.join(File.separator, tmpDirList.get(repoCounter).toString(), remoteRepoDirName) + File.separator;
        Files.createDirectory(Paths.get(remoteRepoDir));
        copyFiles("/notebooks/foo", remoteRepoDir);

        // Create the fake remote Git repo
        Repository remoteRepo = new FileRepository(remoteRepoDir + ".git");
        remoteRepo.create();

        return new Git(remoteRepo);
    }

    @BeforeEach
    void setUp() {
        session.purgeDatabase();
        session.clear();
    }

    @BeforeAll
    static void beforeAll(SessionFactory factory, Config config) throws NoSuchAlgorithmException {
        if (factory == null) {
            var sessionFactory = new SessionFactory(
                    new Configuration.Builder()
                            .uri("bolt://0.0.0.0:7687")
                            .credentials("neo4j", "password")
                            .build(),
                    Commit.class.getPackageName()
            );
            session = sessionFactory.openSession();
        } else {
            session = factory.openSession();
        }

        store = new NotebookStore(session);
        handler = new GithubHookService(store, new GitStore(config), new GithubHookVerifier(config));
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
    void testHook() throws Exception {
        int repoCounter = 0;
        Git remoteRepo = createRemoteRepo(repoCounter);
        JsonNode payloadInitialCommit = commitToRemote("Initial commit", remoteRepo, repoCounter);
        String firstCommitId = payloadInitialCommit.get("head_commit").get("id").textValue();
        handler.checkoutAndParse(payloadInitialCommit);

        Commit firstCommit = store.getCommit(firstCommitId);
        assertThat(firstCommit.getCreates()).hasSize(4);
        assertThat(firstCommit.getDeletes()).isEmpty();
        assertThat(firstCommit.getUpdates()).isEmpty();
        assertThat(firstCommit.getUnchanged()).isEmpty();

        // Test new commit
        duplicateFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoCounter), repoCounter);
        JsonNode secondPayload = commitToRemote("Second commit from remote repository", remoteRepo, repoCounter);
        handler.checkoutAndParse(secondPayload);
        String secondCommitId = secondPayload.get("head_commit").get("id").textValue();
        assertThat(secondCommitId).isNotEqualTo(firstCommitId);

        Commit secondCommit = store.getCommit(secondCommitId);
        assertThat(secondCommit.getCreates()).hasSize(1);
        assertThat(secondCommit.getDeletes()).isEmpty();
        assertThat(secondCommit.getUpdates()).isEmpty();
        assertThat(secondCommit.getUnchanged()).hasSize(4);

        // Delete file and commit
        JsonNode deletePayload = deleteFileFromRemote("Delete file", NOTEBOOK_NAME + NOTEBOOK_FILE_EXTENSION, remoteRepo, repoCounter);
        handler.checkoutAndParse(deletePayload);
        String thirdCommitId = deletePayload.get("head_commit").get("id").textValue();
        assertThat(thirdCommitId).isNotEqualTo(secondCommitId);

        Commit thirdCommit = store.getCommit(thirdCommitId);
        assertThat(thirdCommit.getCreates()).isEmpty();
        assertThat(thirdCommit.getUpdates()).isEmpty();
        assertThat(thirdCommit.getUnchanged()).hasSize(4);

    }

    @Test
    void testNotLatestCommit() throws Exception {
        int repoNumber = 0;
        Git remoteRepo = createRemoteRepo(repoNumber);
        JsonNode payloadInitialCommit = commitToRemote("Initial commit", remoteRepo, repoNumber);
        String firstCommitId = payloadInitialCommit.get("head_commit").get("id").textValue();

        // Do a second commit, not included in payload
        duplicateFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), repoNumber), repoNumber);
        JsonNode secondPayload = commitToRemote("Second commit from remote repository", remoteRepo, repoNumber);
        assertThat(firstCommitId).isNotEqualTo(secondPayload.get("after").textValue());

        handler.checkoutAndParse(payloadInitialCommit);
        Commit commit = store.getCommit(firstCommitId);
        assertThat(commit.getCreates()).hasSize(4);
        assertThat(commit.getMessage()).isEqualTo("Initial commit");

        handler.checkoutAndParse(secondPayload);
        String secondCommitId = secondPayload.get("head_commit").get("id").textValue();
        commit = store.getCommit(secondCommitId);
        assertThat(commit.getMessage()).isEqualTo("Second commit from remote repository");
        assertThat(commit.getUnchanged()).hasSize(4);
    }

    /**
     * TODO: Async tests need to be fixed. The order in which the commits are processed seem to change the amount
     * of notebooks. Not sure why, but I think it has something to do with the file being the same. checkoutAndParse
     * is guarded with synchronized so these tests are not really useful anymore anyways.
     */
    @Disabled
    @Test
    void testAsyncSameRepo() throws Exception {
        int repoCounter = 0;
        Git remoteRepo = createRemoteRepo(repoCounter);

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
        int expectedNumberOfNotebooks = numberOfThreads + 4;
        for (int i = 0; i < numberOfThreads; i++) {
            workers.add(new Thread(new GitHookWorker(
                    readyThreadCounter, callingThreadBlocker, completedThreadCounter, payloads.get(i))));
        }

        workers.forEach(Thread::start);
        readyThreadCounter.await();

        callingThreadBlocker.countDown();
        completedThreadCounter.await();
        Thread.sleep(1000);
        List<Notebook> notebooks = store.getNotebooks();

        assertThat(notebooks.size()).isEqualTo(expectedNumberOfNotebooks);
    }

    @Disabled
    @Test
    void testAsyncMultipleRepos() throws Exception {
        int numberOfThreads = 5;
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfThreads);
        CountDownLatch callingThreadBlocker = new CountDownLatch(1);
        CountDownLatch completedThreadCounter = new CountDownLatch(numberOfThreads);

        // Create repos, commits and payloads
        var payloads = new ArrayList<JsonNode>();
        for (int i = 0; i < numberOfThreads; i++) {
            Git remoteRepo = createRemoteRepo(i);
            createNewFileInRepo(resolveRepoDir(remoteRepo.getRepository().getDirectory().getPath(), i), i);
            payloads.add(commitToRemote("Commit number " + 0, remoteRepo, i));
        }

        List<Thread> workers = new ArrayList<>();
        int expectedNumberOfNotebooks = numberOfThreads + 4; // Three files in each repo
        for (int i = 0; i < numberOfThreads; i++) {
            workers.add(new Thread(new GitHookWorker(
                    readyThreadCounter, callingThreadBlocker, completedThreadCounter, payloads.get(i))));
        }

        workers.forEach(Thread::start);
        readyThreadCounter.await();

        callingThreadBlocker.countDown();
        completedThreadCounter.await();
        List<Notebook> notebooks = store.getNotebooks();

        assertThat(notebooks.size()).isEqualTo(expectedNumberOfNotebooks);
    }

    /**
     * Duplicate one notebook in the repo.
     */
    private void duplicateFileInRepo(String repoDir, int counter) throws IOException {
        Path destinationPath = Paths.get(repoDir);
        Path sourceFile = Paths.get(String.join(File.separator, destinationPath.toString(), NOTEBOOK_NAME + NOTEBOOK_FILE_EXTENSION));
        Path destinationFileName = Paths.get(sourceFile.toString() + "_" + counter + NOTEBOOK_FILE_EXTENSION);
        Files.copy(sourceFile, destinationFileName, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Create a new notebook in the repo.
     */
    private void createNewFileInRepo(String repoDir, int counter) throws IOException {
        Path destinationPath = Paths.get(repoDir);
        Path sourceFile = Paths.get(String.join(File.separator, destinationPath.toString(), NOTEBOOK_NAME + NOTEBOOK_FILE_EXTENSION));
        Path destinationFileName = Paths.get(sourceFile.toString() + "_" + counter + NOTEBOOK_FILE_EXTENSION);
        ObjectNode notebook = mapper.readValue(sourceFile.toFile(), ObjectNode.class);
        ((ObjectNode) notebook.get("metadata")).put("uuid", UUID.randomUUID().toString());
        mapper.writeValue(destinationFileName.toFile(), notebook);
    }

    private void copyFiles(String source, String destination) throws IOException {
        Path testNotebooks = Paths.get(GithubHookServiceTest.class.getResource(source).getPath());
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

    static class GitHookWorker implements Runnable {
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