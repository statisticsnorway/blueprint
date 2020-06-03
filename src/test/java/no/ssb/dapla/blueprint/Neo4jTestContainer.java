package no.ssb.dapla.blueprint;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

@Deprecated
@Testcontainers
public class Neo4jTestContainer implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private Driver driver;
    private String boltUrl;

    @Container
    private static final Neo4jContainer neo4jContainer = new Neo4jContainer().withAdminPassword("");

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        neo4jContainer.stop();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        neo4jContainer.start();
        this.boltUrl = neo4jContainer.getBoltUrl();
        driver = GraphDatabase.driver(boltUrl, AuthTokens.none());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(Object[].class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return new Object[]{driver, boltUrl};
    }
}
