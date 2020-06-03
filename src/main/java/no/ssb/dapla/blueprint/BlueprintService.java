package no.ssb.dapla.blueprint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import no.ssb.dapla.blueprint.git.GitHandler;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.neo4j.driver.Values.parameters;

public class BlueprintService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintService.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Driver driver;

    BlueprintService(Config config, Driver driver) {
        this.driver = driver;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .put("/rev/{rev}", this::putRevisionHandler)
                .get("/rev/{rev}", this::getRevisionHandler)
                .delete("/rev/{rev}", this::deleteRevisionHandler)
                .post("/gitPushHook", this::postGitPushHook)
        ;
    }

    private void putRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.writeTransaction(tx -> {
                Result result = tx.run("CREATE (r:GitRevision) SET r.revision = $rev RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(201).send(node);
        }
    }

    private void getRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            JsonNode node = mapper.valueToTree(session.readTransaction(tx -> {
                Result result = tx.run("MATCH (r:GitRevision {revision: $rev}) RETURN r",
                        parameters("rev", request.path().param("rev")));
                return result.single().get("r").asNode().asMap();
            }));
            response.status(200).send(node);
        }
    }

    private void deleteRevisionHandler(ServerRequest request, ServerResponse response) {
        try (Session session = driver.session()) {
            ResultSummary resultSummary = session.writeTransaction(tx -> {
                Result result = tx.run("MATCH (r:GitRevision {revision: $rev}) OPTIONAL MATCH (r)<--(nb) OPTIONAL MATCH (r)<--(nb)--(ds) DETACH DELETE r, nb, ds",
                        parameters("rev", request.path().param("rev")));
                return result.consume();
            });
            int nodesDeleted = resultSummary.counters().nodesDeleted();
            int relationshipsDeleted = resultSummary.counters().relationshipsDeleted();
            response.status(200).send(mapper.createObjectNode()
                    .put("nodesDeleted", nodesDeleted)
                    .put("relationshipsDeleted", relationshipsDeleted));
        }
    }

    private void postGitPushHook(ServerRequest request, ServerResponse response) {
        try (Session ignored = driver.session()) {
            String secret = ""; // TODO get from env variable
            CompletionStage<JsonNode> payload = request.content().as(JsonNode.class);

            Optional<String> signature = request.headers().value("X-Hub-Signature");

            payload.thenAccept(body -> {
                boolean verified = false;
                if (signature.isPresent()) {
                    // Verify signature
                    verified = GitHandler.verifySignature(signature.get(), secret, body.toString());
                }
                if (verified) {
                    // do stuff

                    // TODO: Implement this as a separate service and add the service in the
                    //   WebServer config/routing.
                    GitHandler handler = new GitHandler(null, null);
                    handler.handleHook(body);
                    // GitHandler.handleHook(body, null);

                    response.status(200).send();
                } else {
                    response.status(Http.Status.FORBIDDEN_403);
                }

            }).exceptionally(t -> {
                response.status(500).send(t.getMessage());
                return null;
            });

        }
    }
}
