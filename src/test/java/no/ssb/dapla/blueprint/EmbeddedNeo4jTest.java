package no.ssb.dapla.blueprint;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class EmbeddedNeo4jTest {

    public static DatabaseManagementService managementService;

    @BeforeAll
    public static void beforeAll() throws IOException {
        Path tempFile = Files.createTempDirectory("dapla-blueprint-db-");
        tempFile.toFile().deleteOnExit();
        managementService = new DatabaseManagementServiceBuilder(tempFile.toFile())
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();
    }

    @AfterAll
    public static void afterAll() {
        managementService.shutdown();
    }

}
