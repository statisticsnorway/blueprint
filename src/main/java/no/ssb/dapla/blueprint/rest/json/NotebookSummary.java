package no.ssb.dapla.blueprint.rest.json;

import no.ssb.dapla.blueprint.neo4j.model.CommittedFile;

public class NotebookSummary {

    protected final CommittedFile delegate;

    public NotebookSummary(CommittedFile delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getNotebook().getBlobId();
    }

    public String getCommitId() {
        return delegate.getCommit().getId();
    }

    public String getPath() {
        return delegate.getPath().toString();
    }

    public String getFetchUrl() {
        return String.format(
                "/api/v1/repositories/%s/commits/%s/notebooks/%s",
                delegate.getCommit().getRepository().getId(),
                delegate.getCommit().getId(),
                delegate.getNotebook().getBlobId()
        );
    }
}
