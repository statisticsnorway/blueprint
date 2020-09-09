package no.ssb.dapla.blueprint.parser;

import freemarker.template.TemplateException;
import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.notebook.Notebook;
import no.ssb.dapla.blueprint.notebook.Repository;
import no.ssb.dapla.blueprint.notebook.Revision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class ParserTest {

    private Parser parser;
    private NotebookStore store;

    private static Notebook createNotebook(String commit, String repositoryURL, String path, Set<String> inputs, Set<String> outputs) {
        Notebook notebook = new Notebook();
        notebook.setPath(path);
        notebook.setInputs(inputs);
        notebook.setOutputs(outputs);

        var revision = new Revision(commit);
        revision.setRepository(new Repository(repositoryURL));
        notebook.setRevision(revision);

        return notebook;
    }

    @BeforeEach
    void setUp(Driver driver) {
        store = new NotebookStore(driver);
        parser = new Parser(new NotebookFileVisitor(Set.of()), new Neo4jOutput(store));
        driver.session().writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));
    }

    @Test
    @Disabled
    void testAirflowOutput() throws IOException, TemplateException {
        var airflowOutput = new AirflowOutput();
        var airflowParser = new Parser(new NotebookFileVisitor(Set.of()), airflowOutput);

        var revision = new Revision("commit2");
        revision.setRepository(new Repository("http://github.com/test/test"));
        airflowParser.parse(Path.of("src/test/resources/notebooks/graph/commit2"), revision);

        airflowOutput.close();

    }

    @Test
    void testCommit1() throws IOException {

        var revision = new Revision("commit1");
        revision.setRepository(new Repository("http://github.com/test/test"));
        parser.parse(Path.of("src/test/resources/notebooks/graph/commit1"), revision);

        Notebook familyNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "Familie.ipynb",
                Set.of("/freg/en", "/freg/to", "/rå/familie", "/skatt/tre", "/skatt/fire"),
                Set.of("/familie/en", "/familie/to")
        );

        Notebook fregNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        List<Notebook> notebooks = store.getNotebooks();
        assertThat(notebooks.get(0)).usingRecursiveComparison().isEqualTo(skattNotebook);
        assertThat(notebooks.get(1)).usingRecursiveComparison().isEqualTo(familyNotebook);
        assertThat(notebooks.get(2)).usingRecursiveComparison().isEqualTo(fregNotebook);
    }

    @Test
    void testCommit2() throws IOException {

        var revision = new Revision("commit2");
        revision.setRepository(new Repository("http://github.com/test/test"));
        parser.parse(Path.of("src/test/resources/notebooks/graph/commit2"), revision);

        Notebook familyNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "Familie.ipynb",
                Set.of("/skatt/en", "/skatt/to", "/freg/tre", "/freg/fire", "/rå/familie"),
                Set.of("/familie/en", "/familie/to", "/familie/tre")
        );

        Notebook fregNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        List<Notebook> notebooks = store.getNotebooks();

        Collections.sort(notebooks, Comparator.comparing(Notebook::getFileName));
        assertThat(notebooks.get(0)).usingRecursiveComparison().isEqualTo(familyNotebook);
        assertThat(notebooks.get(1)).usingRecursiveComparison().isEqualTo(fregNotebook);
        assertThat(notebooks.get(2)).usingRecursiveComparison().isEqualTo(skattNotebook);
    }
}