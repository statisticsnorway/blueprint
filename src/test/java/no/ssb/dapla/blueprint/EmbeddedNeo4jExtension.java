package no.ssb.dapla.blueprint;


import io.helidon.config.Config;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import org.junit.jupiter.api.extension.*;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

import static io.helidon.config.ConfigSources.classpath;

public class EmbeddedNeo4jExtension implements BeforeAllCallback, ParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedNeo4jExtension.class);

    private static final String NEO_KEY = "embedded neo4j";
    private Config config;

    private static ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getRoot().getStore(ExtensionContext.Namespace.create(EmbeddedNeo4jExtension.class));
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        // TODO: Investigate to see if getting the config from another extension is possible.
        this.config = Config.builder(
                classpath("application-dev.yaml"),
                classpath("application.yaml")
        ).metaConfig().build();

        Config neo4jConfig = this.config.get("neo4j");
        String host = neo4jConfig.get("host").asString().get();
        int port = neo4jConfig.get("port").asInt().get();

        Path tempFile = Files.createTempDirectory("junit-neo4j-db-");

        ExtensionContext.Store store = getStore(extensionContext);
        store.getOrComputeIfAbsent(NEO_KEY, key -> new ClosableHolder(tempFile, host, port),
                ClosableHolder.class);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(SessionFactory.class) || type.equals(Config.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(SessionFactory.class)) {
            return getStore(extensionContext).get(NEO_KEY, ClosableHolder.class).factory;
        }
        if (type.equals(Config.class)) {
            return config;
        }
        throw new IllegalStateException("resolveParameter called on unsupported parameter type");
    }

    private static final class ClosableHolder implements ExtensionContext.Store.CloseableResource {

        private final DatabaseManagementService managementService;
        private final File databaseFolder;
        private final SessionFactory factory;

        private ClosableHolder(Path databaseFolder, String host, Integer port) {
            logger.warn("starting up neo4j");
            this.databaseFolder = Objects.requireNonNull(databaseFolder.toFile());
            this.databaseFolder.deleteOnExit();
            this.managementService = new DatabaseManagementServiceBuilder(this.databaseFolder)
                    .setConfig(GraphDatabaseSettings.pagecache_memory, "512M")
                    .setConfig(GraphDatabaseSettings.string_block_size, 60)
                    .setConfig(GraphDatabaseSettings.array_block_size, 300)
                    .setConfig(BoltConnector.enabled, true)
                    .setConfig(BoltConnector.listen_address, new SocketAddress(host, port))
                    .build();

            var builder = new Configuration.Builder();
            builder.uri("bolt://" + host + ":" + port);

            this.factory = new SessionFactory(builder.build(), Commit.class.getPackageName());
        }


        @Override
        public void close() throws Throwable {
            logger.warn("shutting down neo4j");
            factory.close();
            managementService.shutdown();
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
}
