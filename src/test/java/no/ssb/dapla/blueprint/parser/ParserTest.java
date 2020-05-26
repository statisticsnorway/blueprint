package no.ssb.dapla.blueprint.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ParserTest {

    private static DatabaseManagementService managementService;

    @BeforeAll
    static void beforeAll() throws IOException {
        Path tempFile = Files.createTempDirectory("dapla-blueprint-db-");
        tempFile.toFile().deleteOnExit();
        managementService = new DatabaseManagementServiceBuilder(tempFile.toFile())
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();
    }

    @AfterAll
    static void afterAll() {
        managementService.shutdown();
    }

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