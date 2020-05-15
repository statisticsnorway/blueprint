package no.ssb.dapla.blueprint;

import io.helidon.config.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Neo4J {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4J.class);

    static void initializeEmbedded(Config config) {
        LOG.info("Deleting existing Neo4J data-folder");
        long neo4jStart = System.currentTimeMillis();
        Path dataDirectory = Path.of("target/data");
        if (Files.exists(dataDirectory)) {
            deleteFolder(dataDirectory);
        }
        LOG.info("Starting embedded Neo4J... ");
        String host = config.get("host").asString().get();
        int port = config.get("port").asInt().get();
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(dataDirectory.toFile())
                .setConfig(GraphDatabaseSettings.default_database, "testdb")
                .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
                .setConfig(GraphDatabaseSettings.string_block_size, 60)
                .setConfig(GraphDatabaseSettings.array_block_size, 300)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress(host, port))
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(managementService::shutdown));
        LOG.info("Embedded Neo4J started in {} ms", System.currentTimeMillis() - neo4jStart);
    }

    private static void deleteFolder(Path dataDirectory) {
        try {
            Files.walk(dataDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
