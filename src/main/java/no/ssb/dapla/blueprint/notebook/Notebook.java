package no.ssb.dapla.blueprint.notebook;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A simple model of the notebook metadata.
 * <p>
 * Used as a contract btw the parser and the service.
 */
public class Notebook {

    public String repositoryURL;
    public String commitId;
    public String blobId;
    public String fileName;
    public String path;
    public Set<String> inputs = new HashSet<>();
    public Set<String> outputs = new HashSet<>();
    public Boolean changed = true;

    public String getBlobId() {
        return blobId;
    }

    public Boolean getChanged() {
        return changed;
    }

    public String getRepositoryURL() {
        return repositoryURL;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getFileName() {
        return fileName;
    }

    public Set<String> getInputs() {
        return inputs;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notebook notebook = (Notebook) o;
        return fileName.equals(notebook.fileName) &&
                path.equals(notebook.path);
    }

    /**
     * Currently returns the hash code.
     */
    public String getId() {
        return Integer.toHexString(hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, path);
    }

    public String getPath() {
        return path;
    }
}
