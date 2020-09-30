package no.ssb.dapla.blueprint;


import ch.qos.logback.classic.util.ContextInitializer;
import io.helidon.config.Config;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.media.jackson.JacksonSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.openapi.OpenAPISupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebTracingConfig;
import io.helidon.webserver.accesslog.AccessLogSupport;
import io.helidon.webserver.cors.CorsSupport;
import no.ssb.dapla.blueprint.neo4j.GitStore;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.rest.BlueprintService;
import no.ssb.dapla.blueprint.rest.GithubHookService;
import no.ssb.dapla.blueprint.rest.GithubHookVerifier;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
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

    private final Map<Class<?>, Object> instanceByType = new ConcurrentHashMap<>();
    private final NotebookStore notebookStore;
    private final GitStore gitStore;

    public BlueprintApplication(Config config) throws NoSuchAlgorithmException {
        put(Config.class, config);

        SessionFactory driver = initNeo4jDriver(config.get("neo4j"));
        put(SessionFactory.class, driver);

        HealthSupport health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks())
                //.addLiveness(new Neo4jHealthCheck(driver))
                .build();
        MetricsSupport metrics = MetricsSupport.create();

        this.notebookStore = new NotebookStore(driver);
        gitStore = new GitStore(config);

        BlueprintService blueprintService = new BlueprintService(notebookStore, gitStore);
        GithubHookService githubHookService = new GithubHookService(
                notebookStore,
                gitStore,
                new GithubHookVerifier(config)
        );

        var rapidoc = StaticContentSupport.builder("/rapidoc")
                .welcomeFileName("index.html").build();
        var redoc = StaticContentSupport.builder("/redoc")
                .welcomeFileName("index.html").build();
        var swagger = StaticContentSupport.builder("/swagger")
                .welcomeFileName("index.html").build();

        var server = WebServer.builder();

        CorsSupport corsSupport = CorsSupport.create();

        server.routing(Routing.builder()
                .register(AccessLogSupport.create(config.get("server.access-log")))
                .register(WebTracingConfig.create(config.get("tracing")))
                .register(OpenAPISupport.create(config))
                .register(health)
                .register(metrics)
                .register("/swagger", swagger)
                .register("/rapidoc", rapidoc)
                .register("/redoc", redoc)

                .register("/api/v1", corsSupport, blueprintService)
                .register("/api/v1", githubHookService)
        );
        server.addMediaSupport(JacksonSupport.create());

        config.get("server.port").asInt().ifPresent(server::port);
        config.get("server.host").asString().map(s -> {
            try {
                return InetAddress.getByName(s);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }).ifPresent(server::bindAddress);
        put(WebServer.class, server.build());
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) throws NoSuchAlgorithmException {
        BlueprintApplication app = new BlueprintApplication(Config.create());

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        app.get(WebServer.class).start()
                .thenAccept(ws -> {
                    LOG.info("Server up and running: http://{}:{}/api/v1", ws.configuration().bindAddress(), ws.port());
                    ws.whenShutdown().thenRun(()
                            -> System.out.println("WEB server is DOWN. Good bye!"));
                })
                .exceptionally(t -> {
                    System.err.println("Startup failed: " + t.getMessage());
                    t.printStackTrace(System.err);
                    return null;
                });
    }

    public static SessionFactory initNeo4jDriver(String uri, String username, String password, Integer poolSize) {
        var builder = new Configuration.Builder();
        builder.credentials(username, password);
        builder.uri(uri);
        builder.connectionPoolSize(poolSize);
        return new SessionFactory(builder.build(), Commit.class.getPackageName());
    }

    public static SessionFactory initNeo4jDriver(Config config) {
        String host = config.get("host").asString().get();
        int port = config.get("port").asInt().get();
        return initNeo4jDriver(
                "bolt://" + host + ":" + port,
                config.get("username").asString().get(),
                config.get("password").asString().get(),
                config.get("poolSize").asInt().orElse(10)
        );
    }

    public GitStore getGitStore() {
        return gitStore;
    }

    public NotebookStore getNotebookStore() {
        return notebookStore;
    }

    public <T> T put(Class<T> clazz, T instance) {
        return (T) instanceByType.put(clazz, instance);
    }

    public <T> T get(Class<T> clazz) {
        return (T) instanceByType.get(clazz);
    }
}
