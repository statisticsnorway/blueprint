package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.notebook.Notebook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class ParserTest {

    private static Notebook createNotebook(String commit, String repositoryURL, String path, Set<String> inputs, Set<String> outputs) {
        Notebook notebook = new Notebook();
        notebook.commitId = commit;
        notebook.repositoryURL = repositoryURL;
        notebook.fileName = Path.of(path).getFileName().toString();
        notebook.path = path;
        notebook.inputs = inputs;
        notebook.outputs = outputs;
        return notebook;
    }

    @BeforeEach
    void setUp(Driver driver) {
        driver.session().writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n"));
    }

    @Test
    void testCommit1(Driver driver) throws IOException {
        Parser.main(
                "-c", "commit1",
                "--url", "http://github.com/test/test",
                "--host", "bolt://localhost:7687", new File("src/test/resources/notebooks/graph/commit1").toString()
        );
        NotebookStore store = new NotebookStore(driver);

        Notebook familyNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit1/Familie.ipynb",
                Set.of("/freg/en", "/freg/to", "/rå/familie", "/skatt/tre", "/skatt/fire"),
                Set.of("/familie/en", "/familie/to")
        );

        Notebook fregNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit1/Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit1/Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        List<Notebook> notebooks = store.getNotebooks();
        assertThat(notebooks).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(familyNotebook, skattNotebook, fregNotebook);
    }

    @Test
    void testCommit2(Driver driver) throws IOException {
        Parser.main(
                "-c", "commit2",
                "--url", "http://github.com/test/test",
                "--host", "bolt://localhost:7687", new File("src/test/resources/notebooks/graph/commit2").toString()
        );
        NotebookStore store = new NotebookStore(driver);

        Notebook familyNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit2/Familie.ipynb",
                Set.of("/skatt/en", "/skatt/to", "/freg/tre", "/freg/fire", "/rå/familie"),
                Set.of("/familie/en", "/familie/to", "/familie/tre")
        );

        Notebook fregNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit2/Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                "src/test/resources/notebooks/graph/commit2/Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        List<Notebook> notebooks = store.getNotebooks();
        assertThat(notebooks).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        familyNotebook, skattNotebook, fregNotebook
                );
    }
}