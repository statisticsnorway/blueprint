package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.notebook.Notebook;

import java.util.Objects;

public class Neo4jOutput implements Output {

    private final NotebookStore notebookStore;

    public Neo4jOutput(NotebookStore notebookStore) {
        this.notebookStore = Objects.requireNonNull(notebookStore);
    }

    @Override
    public void output(Notebook notebook) {
        notebookStore.addNotebook(notebook);
    }
}
