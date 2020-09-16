package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Dataset;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTest {

    private Parser parser;
    private ArrayList<Notebook> notebooks;

    public static Notebook createNotebook(String commitId, String repositoryURL, String blobId, String path, Set<String> inputs, Set<String> outputs) {
        Notebook notebook = new Notebook();

        // TODO
        //notebook.setPath(path);

        inputs.stream().map(Dataset::new).forEach(dataset -> notebook.getInputs().add(dataset));
        outputs.stream().map(Dataset::new).forEach(dataset -> notebook.getOutputs().add(dataset));

        var commit = new Commit(commitId);
        // TODO commit.setRepository(new Repository(repositoryURL));

        // TODO
        //notebook.setCreateCommit(commit);
        notebook.setBlobId(blobId);

        return notebook;
    }

    @BeforeEach
    void setUp() {
        notebooks = new ArrayList<>();
        parser = new Parser(new NotebookFileVisitor(Set.of()), notebook -> {
            notebooks.add(notebook);
        });
    }

    @Test
    void testCommit1() throws IOException {

        var commit = new Commit("commit1");
        var repository = new Repository("http://github.com/test/test");
        parser.parse(Path.of("src/test/resources/notebooks/graph/commit1"), commit, repository);

        Notebook familyNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                null,
                "Familie.ipynb",
                Set.of("/freg/en", "/freg/to", "/rå/familie", "/skatt/tre", "/skatt/fire"),
                Set.of("/familie/en", "/familie/to")
        );

        Notebook fregNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                null,
                "Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit1",
                "http://github.com/test/test",
                null,
                "Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        // TODO notebooks.sort(Comparator.comparing(Notebook::getPath));
        assertThat(notebooks.get(0)).usingRecursiveComparison().isEqualTo(familyNotebook);
        assertThat(notebooks.get(1)).usingRecursiveComparison().isEqualTo(fregNotebook);
        assertThat(notebooks.get(2)).usingRecursiveComparison().isEqualTo(skattNotebook);
    }

    @Test
    void testCommit2() throws IOException {

        var commit = new Commit("commit2");
        var repository = new Repository("http://github.com/test/test");
        parser.parse(Path.of("src/test/resources/notebooks/graph/commit2"), commit, repository);

        Notebook familyNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                null,
                "Familie.ipynb",
                Set.of("/skatt/en", "/skatt/to", "/freg/tre", "/freg/fire", "/rå/familie"),
                Set.of("/familie/en", "/familie/to", "/familie/tre")
        );

        Notebook fregNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                null,
                "Freg.ipynb",
                Set.of("/rå/freg/en", "/rå/freg/to", "/rå/freg/tre", "/rå/freg/fire"),
                Set.of("/freg/en", "/freg/to", "/freg/tre", "/freg/fire")
        );

        Notebook skattNotebook = createNotebook(
                "commit2",
                "http://github.com/test/test",
                null,
                "Skatt.ipynb",
                Set.of("/rå/skatt/en", "/rå/skatt/to", "/rå/skatt/tre", "/rå/skatt/fire"),
                Set.of("/skatt/en", "/skatt/to", "/skatt/tre", "/skatt/fire")
        );

        // TODO notebooks.sort(Comparator.comparing(Notebook::getPath));
        assertThat(notebooks.get(0)).usingRecursiveComparison().isEqualTo(familyNotebook);
        assertThat(notebooks.get(1)).usingRecursiveComparison().isEqualTo(fregNotebook);
        assertThat(notebooks.get(2)).usingRecursiveComparison().isEqualTo(skattNotebook);
    }
}