package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.RelationshipEntity;

import java.nio.file.Path;

@RelationshipEntity(type = "CREATES")
public class CreatedFile extends CommittedFile {

    public CreatedFile(Commit commit, Path path, Notebook notebook) {
        super(commit, path, notebook);
    }
}
