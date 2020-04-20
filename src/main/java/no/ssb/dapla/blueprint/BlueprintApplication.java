package no.ssb.dapla.blueprint;


import ch.qos.logback.classic.util.ContextInitializer;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jackson.server.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.openapi.OpenAPISupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebTracingConfig;
import io.helidon.webserver.accesslog.AccessLogSupport;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;

public class BlueprintApplication {

    private static final Logger LOG;

    static {
        String logbackConfigurationFile = System.getenv("LOGBACK_CONFIGURATION_FILE");
        if (logbackConfigurationFile != null) {
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, logbackConfigurationFile);
        }
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        LOG = LoggerFactory.getLogger(BlueprintApplication.class);
    }

    public static void initLogging() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {
        BlueprintApplication app = new BlueprintApplication(Config.create());

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        app.get(WebServer.class).start()
                .thenAccept(ws -> {
                    System.out.println(
                            "WEB server is up! http://localhost:" + ws.port() + "/greet");
                    ws.whenShutdown().thenRun(()
                            -> System.out.println("WEB server is DOWN. Good bye!"));
                })
                .exceptionally(t -> {
                    System.err.println("Startup failed: " + t.getMessage());
                    t.printStackTrace(System.err);
                    return null;
                });
    }

    private final Map<Class<?>, Object> instanceByType = new ConcurrentHashMap<>();

    BlueprintApplication(Config config) {
        put(Config.class, config);

        Driver driver = initNeo4jDriver(config.get("neo4j"));
        put(Driver.class, driver);

        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())   // Adds a convenient set of checks
                .build();
        MetricsSupport metrics = MetricsSupport.create();

        BlueprintService blueprintService = new BlueprintService(config, driver);

        WebServer server = WebServer.create(ServerConfiguration.create(config.get("server")), Routing.builder()
                .register(AccessLogSupport.create(config.get("server.access-log")))
                .register(WebTracingConfig.create(config.get("tracing")))
                .register(JacksonSupport.create())
                .register(OpenAPISupport.create(config))
                .register(health)  // "/health"
                .register(metrics) // "/metrics"
                .register("/blueprint", blueprintService)
                .build());
        put(WebServer.class, server);
    }

    private Driver initNeo4jDriver(Config config) {
        String host = config.get("host").asString().get();
        int port = config.get("port").asInt().get();
        String username = config.get("username").asString().get();
        String password = config.get("password").asString().get();
        return GraphDatabase.driver("bolt://" + host + ":" + port, AuthTokens.basic(username, password));
    }

    public <T> T put(Class<T> clazz, T instance) {
        return (T) instanceByType.put(clazz, instance);
    }

    public <T> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz);
    }
}
