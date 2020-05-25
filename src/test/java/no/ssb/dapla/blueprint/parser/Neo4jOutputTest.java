package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.notebook.Notebook;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.List;

class Neo4jOutputTest {

    private static final List<String> DS_ONE = List.of(
            "/ds/one/one",
            "/ds/one/two",
            "/ds/one/three",
            "/ds/one/four"
    );

    private static final List<String> DS_TWO = List.of(
            "/ds/two/one",
            "/ds/two/two",
            "/ds/two/three",
            "/ds/two/four"
    );

    private static final List<String> DS_THREE = List.of(
            "/ds/three/one",
            "/ds/three/two",
            "/ds/three/three",
            "/ds/three/four"
    );

    @Test
    void testInsertNotebook() {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687");
        Neo4jOutput output = new Neo4jOutput(driver);

        Notebook notebook = new Notebook();

        notebook.commitId = "commitId";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";

        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        output.output(notebook);
    }

    @Test
    void testInsertNotebooksWithMatchingInputOutput() {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687");
        Neo4jOutput output = new Neo4jOutput(driver);

        Notebook notebook = new Notebook();

        notebook.commitId = "commitId";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";
        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        output.output(notebook);

        notebook.commitId = "commitId";
        notebook.path = "/some/other/path";
        notebook.fileName = "/some/other/path";
        notebook.inputs = DS_TWO;
        notebook.outputs = DS_THREE;
        output.output(notebook);

    }

    @Test
    void testInsertWithMatchingInputOutputDifferentCommits() {
        Driver driver = GraphDatabase.driver("bolt://localhost:7687");
        Neo4jOutput output = new Neo4jOutput(driver);

        Notebook notebook = new Notebook();

        notebook.commitId = "commitId1";
        notebook.path = "/some/path";
        notebook.fileName = "/some/path";
        notebook.inputs = DS_ONE;
        notebook.outputs = DS_TWO;

        output.output(notebook);

        notebook.commitId = "commitId2";
        notebook.path = "/some/other/path";
        notebook.fileName = "/some/other/path";
        notebook.inputs = DS_TWO;
        notebook.outputs = DS_THREE;
        output.output(notebook);

    }




}