package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.RelationshipEntity;

import java.nio.file.Path;

@RelationshipEntity(type = "UNCHANGED")
public class UnchangedFile extends CommittedFile {
    public UnchangedFile(Commit commit, Path path, Notebook notebook) {
        super(commit, path, notebook);
    }
}
