package no.ssb.dapla.blueprint.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.Objects;

public class Neo4jHealthCheck implements HealthCheck {

    private final Driver driver;

    public Neo4jHealthCheck(Driver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder response = HealthCheckResponse.builder().name("neo4j").up();
        try {
            driver.session().run("""
                    MATCH(n)
                    RETURN count(n) AS count
                    """
            ).stream().findAny().map(r -> r.get("count").asInt())
                    .ifPresent(nodeCount -> response.withData("nodeCount", nodeCount));
        } catch (ServiceUnavailableException sue) {
            response.down().withData("message", sue.getMessage());
        }
        return response.build();
    }

}
