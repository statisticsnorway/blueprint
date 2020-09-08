package no.ssb.dapla.blueprint;

import no.ssb.dapla.blueprint.notebook.Notebook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.util.List;
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

    @Test
    void testInsertNotebook() {
        Notebook notebook = new Notebook();

        notebook.repositoryURL = "repo";
        notebook.commitId = "commitId";
        notebook.blobId = "blobId";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";

        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        store.addNotebook(notebook);
    }

    @Test
    void testInsertNotebooksWithMatchingInputOutput() {
        Notebook notebook = new Notebook();

        notebook.repositoryURL = "repo";
        notebook.commitId = "commitId";
        notebook.blobId = "blobId";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";
        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        store.addNotebook(notebook);

        notebook.repositoryURL = "repo";
        notebook.commitId = "commitId";
        notebook.blobId = "blobId1";
        notebook.path = "/some/other/path";
        notebook.fileName = "/some/other/path";
        notebook.inputs = DS_TWO;
        notebook.outputs = DS_THREE;
        store.addNotebook(notebook);

    }

    @Test
    void testInsertWithMatchingInputOutputDifferentCommits() {

        Notebook notebook = new Notebook();

        notebook.repositoryURL = "repo";
        notebook.commitId = "commitId1";
        notebook.blobId = "blobId1";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";
        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        store.addNotebook(notebook);

        notebook.repositoryURL = "repo";
        notebook.commitId = "commitId2";
        notebook.blobId = "blobId2";
        notebook.path = "/some/other/path";
        notebook.fileName = "/some/other/path";
        notebook.inputs = DS_TWO;
        notebook.outputs = DS_THREE;
        store.addNotebook(notebook);

    }

    @Test
    void testQueryDiff() {

        Notebook notebook = new Notebook();

        notebook.repositoryURL = "repo";
        notebook.commitId = "changedCommit";
        notebook.blobId = "blobId";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";
        notebook.changed = true;
        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        store.addNotebook(notebook);

        notebook.repositoryURL = "repo";
        notebook.commitId = "changedCommit";
        notebook.blobId = "blobId";
        notebook.path = "/some/other/path";
        notebook.fileName = "/some/other/path";
        notebook.changed = true;
        notebook.inputs = DS_TWO;
        notebook.outputs = DS_THREE;
        store.addNotebook(notebook);


    }
}