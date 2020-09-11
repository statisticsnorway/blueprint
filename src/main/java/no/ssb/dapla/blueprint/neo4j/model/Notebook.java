package no.ssb.dapla.blueprint.neo4j.model;

import no.ssb.dapla.blueprint.neo4j.converters.PathStringConverter;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.nio.file.Path;
import java.util.*;

@NodeEntity
public class Notebook {

    @Id
    private String blobId;

    @Convert(PathStringConverter.class)
    private Path path;

    @Relationship(type = "CREATES", direction = Relationship.INCOMING)
    private Commit createdIn;

    @Relationship(type = "UPDATES", direction = Relationship.INCOMING)
    private Set<Commit> updatedIn = new HashSet<>();

    @Relationship(type = "PRODUCES")
    private Set<Dataset> outputs = new HashSet<>();
    @Relationship(type = "CONSUMES")
    private Set<Dataset> inputs = new HashSet<>();

    @Transient
    private Boolean changed = false;

    public Notebook() {
    }

    public Notebook(String blobId) {
        this.blobId = Objects.requireNonNull(blobId);
    }

    public Boolean isChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public Set<Dataset> getOutputs() {
        return outputs;
    }

    public void addOutputs(Dataset... datasets) {
        addOutputs(Arrays.asList(datasets));
    }

    public void addOutputs(Collection<? extends Dataset> datasets) {
        this.outputs.addAll(datasets);
    }

    public Set<Dataset> getInputs() {
        return inputs;
    }

    public void addInputs(Dataset... datasets) {
        addInputs(Arrays.asList(datasets));
    }

    public void addInputs(Collection<? extends Dataset> datasets) {
        this.inputs.addAll(datasets);
    }

    public String getBlobId() {
        return blobId;
    }

    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = Path.of(path);
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Commit getCreatedIn() {
        return createdIn;
    }

    public Set<Commit> getUpdatedIn() {
        return updatedIn;
    }

    public void setCreateCommit(Commit commit) {
        createdIn = Objects.requireNonNull(commit);
        commit.addCreate(this);
    }

    public void setUpdateCommit(Commit commit) {
        updatedIn.add(commit);
        commit.addUpdate(this);
    }
}
