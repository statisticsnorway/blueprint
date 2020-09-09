package no.ssb.dapla.blueprint.neo4j.model;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple model of the notebook metadata.
 * <p>
 * Used as a contract btw the parser and the service.
 */
public class Notebook {

    private String blobId = "";
    private Path path;
    private Set<String> inputs = new HashSet<>();
    private Set<String> outputs = new HashSet<>();
    private Boolean changed = true;
    private Revision revision;

    public Revision getRevision() {
        return revision;
    }

    public void setRevision(Revision revision) {
        this.revision = revision;
    }

    public String getBlobId() {
        return blobId;
    }

    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }

    public Boolean isChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }

    public URI getRepositoryUri() {
        return getRevision().getRepository().getUri();
    }

    public String getCommitId() {
        return getRevision().getId();
    }

    public Path getFileName() {
        return getPath().getFileName();
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public void setInputs(Set<String> inputs) {
        this.inputs = inputs;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<String> outputs) {
        this.outputs = outputs;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setPath(String path) {
        setPath(Path.of(path));
    }
}
