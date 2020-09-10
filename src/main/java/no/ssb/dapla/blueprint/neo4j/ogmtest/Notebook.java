package no.ssb.dapla.blueprint.neo4j.ogmtest;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.Objects;
import java.util.Set;

@NodeEntity
public class Notebook {

    @Id
    private String blobId;

    private String path;

    @Relationship(type = "CREATES", direction = Relationship.INCOMING)
    private Commit createdIn;

    @Relationship(type = "UPDATES", direction = Relationship.INCOMING)
    private Set<Commit> updatedIn;
    @Relationship(type = "PRODUCES")
    private Set<Dataset> produces;
    @Relationship(type = "CONSUMES")
    private Set<Dataset> consumes;

    // Needed by ogm
    private Notebook() {
    }

    public Notebook(String blobId) {
        this.blobId = Objects.requireNonNull(blobId);
    }

    public Set<Dataset> getProduces() {
        return produces;
    }

    public void setProduces(Set<Dataset> produces) {
        this.produces = produces;
    }

    public Set<Dataset> getConsumes() {
        return consumes;
    }

    public void setConsumes(Set<Dataset> consumes) {
        this.consumes = consumes;
    }

    public String getBlobId() {
        return blobId;
    }

    private void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Commit getCreatedIn() {
        return createdIn;
    }

    public void setCreatedIn(Commit createdIn) {
        this.createdIn = createdIn;
    }

    public Set<Commit> getUpdatedIn() {
        return updatedIn;
    }

    public void setUpdatedIn(Set<Commit> updatedIn) {
        this.updatedIn = updatedIn;
    }
}
