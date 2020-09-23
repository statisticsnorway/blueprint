package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.RelationshipEntity;

import java.nio.file.Path;

@RelationshipEntity(type = "DELETES")
public class DeletedFile extends CommittedFile {

    private DeletedFile() {
    }

    public DeletedFile(Commit commit, Path path, Notebook notebook) {
        super(commit, path, notebook);
    }
}
