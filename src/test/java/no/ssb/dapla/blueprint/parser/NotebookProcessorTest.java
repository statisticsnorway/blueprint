package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.notebook.Notebook;
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
        Notebook notebook = processor.process(loadFile("notebooks/notebook-with-metadata.ipynb"));
        assertThat(notebook.fileName).isEqualTo("notebook-with-metadata.ipynb");
        assertThat(notebook.path).endsWith("/notebooks/notebook-with-metadata.ipynb");
        assertThat(notebook.inputs).contains(
                "/some/input/path", "/some/other/input/path"
        );
        assertThat(notebook.outputs).contains(
                "/some/path", "/some/other/path"
        );
    }

    @Test
    void testHandlesWeirdCharacters() throws IOException {
        // Test against some of the strings from
        // https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
        Notebook notebook = processor.process(loadFile("notebooks/notebook-with-weird-metadata.ipynb"));
        assertThat(notebook.fileName).isEqualTo("notebook-with-weird-metadata.ipynb");
        assertThat(notebook.path).endsWith("/notebooks/notebook-with-weird-metadata.ipynb");
        assertThat(notebook.inputs).isNotEmpty();
        assertThat(notebook.outputs).isNotEmpty();
    }

    @Test
    void testSupportsMissingMetadata() throws IOException {
        Notebook notebook = processor.process(loadFile("notebooks/notebook-without-metadata.ipynb"));
        assertThat(notebook.fileName).isEqualTo("notebook-without-metadata.ipynb");
        assertThat(notebook.path).endsWith("/notebooks/notebook-without-metadata.ipynb");
        assertThat(notebook.inputs).isEmpty();
        assertThat(notebook.outputs).isEmpty();
    }

    @Test
    void testSupportsEmptyMetadata() throws IOException {
        Notebook notebook = processor.process(loadFile("notebooks/notebook-with-empty-metadata.ipynb"));
        assertThat(notebook.fileName).isEqualTo("notebook-with-empty-metadata.ipynb");
        assertThat(notebook.path).endsWith("/notebooks/notebook-with-empty-metadata.ipynb");
        assertThat(notebook.inputs).isEmpty();
        assertThat(notebook.outputs).isEmpty();
    }
}