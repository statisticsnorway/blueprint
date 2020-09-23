package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;

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

    public void saveCommit(Commit commit) {
        notebookStore.saveCommit(commit);
    }

    public void saveRepository(Repository repository) {
        notebookStore.saveRepository(repository);
    }

    public NotebookStore getNotebookStore() {
        return notebookStore;
    }

}
