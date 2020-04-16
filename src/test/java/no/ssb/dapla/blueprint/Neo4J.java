package no.ssb.dapla.blueprint;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Neo4J {

    private static final Logger LOG = LoggerFactory.getLogger(Neo4J.class);

    static void initializeEmbedded() {
        LOG.info("Deleting existing Neo4J data-folder");
        long neo4jStart = System.currentTimeMillis();
        File dataDirectory = new File("target/data");
        if (dataDirectory.isDirectory()) {
            deleteFolder(dataDirectory);
        }
        LOG.info("Starting embedded Neo4J... ");
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(dataDirectory)
                .setConfig(GraphDatabaseSettings.default_database, "testdb")
                .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
                .setConfig(GraphDatabaseSettings.string_block_size, 60)
                .setConfig(GraphDatabaseSettings.array_block_size, 300)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();
        Runtime.getRuntime().addShutdownHook(new Thread(managementService::shutdown));
        LOG.info("Embedded Neo4J started in {} ms", System.currentTimeMillis() - neo4jStart);
    }

    private static void deleteFolder(File dataDirectory) {
        try {
            Files.walkFileTree(dataDirectory.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                        throws IOException {
                    if (e == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw e;
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
