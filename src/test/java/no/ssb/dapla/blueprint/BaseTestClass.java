package no.ssb.dapla.blueprint;

import org.neo4j.driver.Driver;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseTestClass {

    protected void assertParams(Object[] params) {
        assertThat(params.length).as("Params must contain two elements").isEqualTo(2);
        assertThat(params[0]).as("Params cannot be null").isNotNull();
        assertThat(params[1]).as("Params cannot be null").isNotNull();
        assertThat(params[0]).as("First param must be Neo4J driver").isInstanceOf(Driver.class);
        assertThat(params[1]).as("First param must be Neo4J driver").isInstanceOf(String.class);
    }
}
