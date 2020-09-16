package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.RelationshipEntity;

import java.nio.file.Path;

@RelationshipEntity(type = "UPDATES")
public class UpdatedFile extends CommittedFile {
    public UpdatedFile(Commit commit, Path path, Notebook notebook) {
        super(commit, path, notebook);
    }
}
