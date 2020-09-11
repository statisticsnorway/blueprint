package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.model.Notebook;

public interface Output {
    void output(Notebook notebook);
}
