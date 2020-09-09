package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class NotebookProcessorTest {

    NotebookProcessor processor = new NotebookProcessor(new ObjectMapper());

    static File loadFile(String name) {
        return new File("src/test/resources/" + name);
    }

    @Test
    void testCanParseNotebook() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-metadata.ipynb");
        assertThat(notebook.getFileName().toString()).isEqualTo("notebook-with-metadata.ipynb");
        assertThat(notebook.getPath().toString()).endsWith("foo/notebook-with-metadata.ipynb");
        assertThat(notebook.getInputs()).contains(
                "/some/input/path", "/some/other/input/path"
        );
        assertThat(notebook.getOutputs()).contains(
                "/some/path", "/some/other/path"
        );
    }

    @Test
    void testHandlesWeirdCharacters() throws IOException {
        // Test against some of the strings from
        // https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-weird-metadata.ipynb");
        assertThat(notebook.getFileName().toString()).isEqualTo("notebook-with-weird-metadata.ipynb");
        assertThat(notebook.getPath().toString()).endsWith("foo/notebook-with-weird-metadata.ipynb");
        assertThat(notebook.getInputs()).isNotEmpty();
        assertThat(notebook.getOutputs()).isNotEmpty();
    }

    @Test
    void testSupportsMissingMetadata() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-without-metadata.ipynb");
        assertThat(notebook.getFileName().toString()).isEqualTo("notebook-without-metadata.ipynb");
        assertThat(notebook.getPath().toString()).endsWith("foo/notebook-without-metadata.ipynb");
        assertThat(notebook.getInputs()).isEmpty();
        assertThat(notebook.getOutputs()).isEmpty();
    }

    @Test
    void testSupportsEmptyMetadata() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-empty-metadata.ipynb");
        assertThat(notebook.getFileName().toString()).isEqualTo("notebook-with-empty-metadata.ipynb");
        assertThat(notebook.getPath().toString()).endsWith("foo/notebook-with-empty-metadata.ipynb");
        assertThat(notebook.getInputs()).isEmpty();
        assertThat(notebook.getOutputs()).isEmpty();
    }
}