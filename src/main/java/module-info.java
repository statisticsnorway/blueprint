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
    requires io.helidon.media.jackson.common;
    requires io.helidon.media.jackson.server;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires io.helidon.openapi;
    requires io.helidon.webserver.accesslog;
    requires org.neo4j.driver;
    requires java.annotation;
    requires io.helidon.media.jsonp.common;
    requires org.eclipse.jgit;

    requires info.picocli;
    opens no.ssb.dapla.blueprint.parser to info.picocli;

    exports no.ssb.dapla.blueprint;
    exports no.ssb.dapla.blueprint.notebook;
}