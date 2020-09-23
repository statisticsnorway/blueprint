package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugOutput implements Output {

    private static final Logger log = LoggerFactory.getLogger(DebugOutput.class);

    public void output(Notebook notebook) {
        log.debug("Notebook {}", notebook.getBlobId());
    }
}
