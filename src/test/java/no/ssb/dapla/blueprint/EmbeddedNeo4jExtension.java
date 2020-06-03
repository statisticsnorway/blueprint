package no.ssb.dapla.blueprint;


import io.helidon.config.Config;
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

import static io.helidon.config.ConfigSources.classpath;

public class EmbeddedNeo4jExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private DatabaseManagementService managementService;
    private File databaseFolder;
    private Driver driver;
    private Config config;

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

        this.config = Config.builder(
                classpath("application-dev.yaml"),
                classpath("application.yaml")
        ).metaConfig().build();

        Config neo4jConfig = this.config.get("neo4j");
        String host = neo4jConfig.get("host").asString().get();
        int port = neo4jConfig.get("port").asInt().get();

        Path tempFile = Files.createTempDirectory("junit-neo4j-db-");
        databaseFolder = tempFile.toFile();
        databaseFolder.deleteOnExit();

        managementService = new DatabaseManagementServiceBuilder(databaseFolder)
                .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
                .setConfig(GraphDatabaseSettings.string_block_size, 60)
                .setConfig(GraphDatabaseSettings.array_block_size, 300)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress(host, port))
                .build();
        driver = GraphDatabase.driver("bolt://" + host + ":" + port);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(Driver.class) || type.equals(Config.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(Driver.class)) {
            return driver;
        }
        if (type.equals(Config.class)) {
            return config;
        }
        return driver;
    }
}
