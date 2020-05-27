package no.ssb.dapla.blueprint;


import org.junit.jupiter.api.extension.*;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class EmbeddedNeo4jExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private DatabaseManagementService managementService;
    private File databaseFolder;
    private Driver driver;

    @Override
    public void afterAll(ExtensionContext extensionContext) throws IOException {
        if (managementService != null) {
            managementService.shutdown();
        }

        if (databaseFolder != null) {
            Files.walk(databaseFolder.toPath())
                    .sorted(Comparator.reverseOrder()).forEach(t -> {
                try {
                    Files.delete(t);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Path tempFile = Files.createTempDirectory("junit-neo4j-db-");
        databaseFolder = tempFile.toFile();
        databaseFolder.deleteOnExit();

        managementService = new DatabaseManagementServiceBuilder(databaseFolder)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
                .setConfig(GraphDatabaseSettings.string_block_size, 60)
                .setConfig(GraphDatabaseSettings.array_block_size, 300)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();
        driver = GraphDatabase.driver("bolt://localhost:7687");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(Driver.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return driver;
    }
}
