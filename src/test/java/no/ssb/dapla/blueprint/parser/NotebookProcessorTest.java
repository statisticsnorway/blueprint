package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class NotebookProcessorTest {

    NotebookProcessor processor = new NotebookProcessor(new ObjectMapper());

    static File loadFile(String name) {
        return new File("src/test/resources/" + name);
    }

    @Test
    void testCanParseNotebook() throws IOException {
        processor.process(loadFile("notebooks/notebook-with-metadata.ipynb"));
    }

    @Test
    void testHandlesWeirdCharacters() throws IOException {
        // Test against some of the strings from
        // https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
        processor.process(loadFile("notebooks/notebook-with-weird-metadata.ipynb"));
    }

    @Test
    void testSupportsMissingMetadata() throws IOException {
        processor.process(loadFile("notebooks/notebook-without-metadata.ipynb"));
    }

    @Test
    void testSupportsEmptyMetadata() throws IOException {
        processor.process(loadFile("notebooks/notebook-with-empty-metadata.ipynb"));
    }
}