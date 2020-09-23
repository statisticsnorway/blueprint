package no.ssb.dapla.blueprint.rest.json;

import no.ssb.dapla.blueprint.neo4j.model.CommittedFile;
import no.ssb.dapla.blueprint.neo4j.model.Dataset;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class NotebookDetail extends NotebookSummary {

    public NotebookDetail(CommittedFile delegate) {
        super(delegate);
    }

    public Set<String> getInputs() {
        return delegate.getNotebook().getInputs().stream()
                .map(Dataset::getPath)
                .map(Path::toString)
                .collect(Collectors.toSet());
    }

    public Set<String> getOutputs() {
        return delegate.getNotebook().getOutputs().stream()
                .map(Dataset::getPath)
                .map(Path::toString)
                .collect(Collectors.toSet());
    }
}
