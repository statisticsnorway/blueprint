package no.ssb.dapla.blueprint.rest.json;

import no.ssb.dapla.blueprint.neo4j.model.Commit;

import java.util.List;
import java.util.stream.Collectors;

public class CommitDetail extends CommitSummary {

    public CommitDetail(Commit delegate) {
        super(delegate);
    }

    public List<NotebookSummary> getUpdated() {
        return delegate.getUpdates().stream().map(NotebookSummary::new).collect(Collectors.toList());
    }

    public List<NotebookSummary> getCreated() {
        return delegate.getCreates().stream().map(NotebookSummary::new).collect(Collectors.toList());
    }

    public List<NotebookSummary> getDeleted() {
        return delegate.getDeletes().stream().map(NotebookSummary::new).collect(Collectors.toList());
    }
}
