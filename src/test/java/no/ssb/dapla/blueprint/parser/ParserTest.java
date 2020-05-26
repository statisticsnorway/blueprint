package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;

@ExtendWith(EmbeddedNeo4jExtension.class)
class ParserTest {

    @Test
    void testCommit1() throws IOException {
        Parser.main(
                "-c", "commit1",
                "--host", "bolt://localhost:7687", new File("src/test/resources/notebooks/graph/commit1").toString()
        );
    }

    @Test
    void testCommit2() throws IOException {
        Parser.main(
                "-c", "commit2",
                "--host", "bolt://localhost:7687", new File("src/test/resources/notebooks/graph/commit2").toString()
        );
    }
}