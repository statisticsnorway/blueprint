package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.model.Dataset;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NotebookProcessorTest {

    NotebookProcessor processor = new NotebookProcessor();

    @Test
    void testCanParseNotebook() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-metadata.ipynb");

        // TODO assertThat(notebook.getPath().toString()).isEqualTo("foo/notebook-with-metadata.ipynb");

        assertThat(notebook.getInputs()).extracting(Dataset::getPath).extracting(Path::toString)
                .contains(
                        "/some/input/path", "/some/other/input/path"
                );
        assertThat(notebook.getOutputs()).extracting(Dataset::getPath).extracting(Path::toString)
                .contains(
                        "/some/path", "/some/other/path"
                );
    }

    @Test
    void testHandlesWeirdCharacters() throws IOException {
        // Test against some of the strings from
        // https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-weird-metadata.ipynb");
        assertThat(notebook.getInputs()).isNotEmpty();
        assertThat(notebook.getOutputs()).isNotEmpty();
    }

    @Test
    void testSupportsMissingMetadata() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-without-metadata.ipynb");
        assertThat(notebook.getInputs()).isEmpty();
        assertThat(notebook.getOutputs()).isEmpty();
    }

    @Test
    void testSupportsEmptyMetadata() throws IOException {
        Notebook notebook = processor.process("src/test/resources/notebooks", "foo/notebook-with-empty-metadata.ipynb");
        assertThat(notebook.getInputs()).isEmpty();
        assertThat(notebook.getOutputs()).isEmpty();
    }
}