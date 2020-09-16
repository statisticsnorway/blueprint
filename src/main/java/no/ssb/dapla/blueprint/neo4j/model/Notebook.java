package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.*;

@NodeEntity
public class Notebook {

    @Id
    private String blobId;

    @Relationship(type = "PRODUCES")
    private Set<Dataset> outputs = new HashSet<>();

    @Relationship(type = "CONSUMES")
    private Set<Dataset> inputs = new HashSet<>();

    public Notebook() {
    }

    public Notebook(String blobId) {
        this.blobId = Objects.requireNonNull(blobId);
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

}
