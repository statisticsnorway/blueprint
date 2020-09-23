module no.ssb.blueprint {

    requires io.helidon.webserver;
    requires io.helidon.health;
    requires java.logging;
    requires io.helidon.health.checks;
    requires io.helidon.metrics;
    requires org.slf4j;
    requires jul.to.slf4j;
    requires logback.classic;
    requires jdk.unsupported;
    requires io.helidon.media.jackson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires io.helidon.openapi;
    requires io.helidon.webserver.accesslog;
    requires org.neo4j.driver;
    requires java.annotation;
    requires org.eclipse.jgit;

    requires info.picocli;
    requires org.neo4j.ogm.core;
    requires org.neo4j.ogm.drivers.api;
    requires io.github.classgraph;

    opens no.ssb.dapla.blueprint.parser to info.picocli;
    opens no.ssb.dapla.blueprint.neo4j.model to org.neo4j.ogm.core;
    opens no.ssb.dapla.blueprint.neo4j.converters to org.neo4j.ogm.core;
    opens no.ssb.dapla.blueprint.rest.json to com.fasterxml.jackson.databind;

    exports no.ssb.dapla.blueprint;
    exports no.ssb.dapla.blueprint.neo4j;
    exports no.ssb.dapla.blueprint.neo4j.model;
}