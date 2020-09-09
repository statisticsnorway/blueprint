package no.ssb.dapla.blueprint;

import no.ssb.dapla.blueprint.notebook.Notebook;
import no.ssb.dapla.blueprint.notebook.Repository;
import no.ssb.dapla.blueprint.notebook.Revision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.util.Set;

@ExtendWith(EmbeddedNeo4jExtension.class)
class NotebookStoreTest {

    private static final Set<String> DS_ONE = Set.of(
            "/ds/one/one",
            "/ds/one/two",
            "/ds/one/three",
            "/ds/one/four"
    );

    private static final Set<String> DS_TWO = Set.of(
            "/ds/two/one",
            "/ds/two/two",
            "/ds/two/three",
            "/ds/two/four"
    );

    private static final Set<String> DS_THREE = Set.of(
            "/ds/three/one",
            "/ds/three/two",
            "/ds/three/three",
            "/ds/three/four"
    );

    private NotebookStore store;

    @BeforeEach
    void setUp(Driver driver) {
        store = new NotebookStore(driver);
    }

    private Notebook createNotebook(String repositoryUri, String commitId, String blobId, String path,
                                    Set<String> inputs, Set<String> outputs) {

        var repository = new Repository(repositoryUri);
        var revision = new Revision(commitId);
        var notebook = new Notebook();
        notebook.setRevision(revision);
        revision.setRepository(repository);

        notebook.setBlobId(blobId);
        notebook.setPath(path);
        notebook.setInputs(inputs);
        notebook.setOutputs(outputs);

        return notebook;
    }

    @Test
    void testInsertNotebook() {
        Notebook notebook = createNotebook(
                "repo",
                "commitId",
                "blobId",
                "/some/path",
                DS_ONE,
                DS_TWO
        );
        store.addNotebook(notebook);
    }

    @Test
    void testInsertNotebooksWithMatchingInputOutput() {
        Notebook notebook = createNotebook(
                "repo",
                "commitId",
                "blobId",
                "/some/path",
                DS_ONE,
                DS_TWO
        );

        store.addNotebook(notebook);

        notebook = createNotebook(
                "repo",
                "commitId",
                "blobId",
                "/some/path",
                DS_TWO,
                DS_THREE
        );
        store.addNotebook(notebook);

    }

    @Test
    void testInsertWithMatchingInputOutputDifferentCommits() {

        Notebook notebook = createNotebook(
                "repo",
                "commitId1",
                "blobId1",
                "/some/path",
                DS_ONE,
                DS_TWO
        );

        store.addNotebook(notebook);

        notebook = createNotebook(
                "repo",
                "commitId2",
                "blobId2",
                "/some/other/path",
                DS_TWO,
                DS_THREE
        );
        store.addNotebook(notebook);

    }

    @Test
    void testQueryDiff() {

        Notebook notebook = createNotebook(
                "repo",
                "changedCommit",
                "blobId",
                "/some/path",
                DS_ONE,
                DS_TWO
        );
        notebook.setChanged(true);
        store.addNotebook(notebook);

        notebook = createNotebook(
                "repo",
                "changedCommit",
                "blobId",
                "/some/other/path",
                DS_TWO,
                DS_THREE
        );
        notebook.setChanged(true);
        store.addNotebook(notebook);


    }
}