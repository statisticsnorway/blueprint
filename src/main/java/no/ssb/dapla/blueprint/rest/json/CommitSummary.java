package no.ssb.dapla.blueprint.rest.json;

import java.time.Instant;

public class CommitSummary {

    private final no.ssb.dapla.blueprint.neo4j.model.Commit delegate;

    public CommitSummary(no.ssb.dapla.blueprint.neo4j.model.Commit delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public String getAuthorName() {
        return delegate.getAuthorName();
    }

    public String getAuthorEmail() {
        return delegate.getAuthorEmail();
    }

    public Instant getAuthoredAt() {
        return delegate.getAuthoredAt();
    }

    public String getCommitterName() {
        return delegate.getCommitterName();
    }

    public String getCommitterEmail() {
        return delegate.getCommitterEmail();
    }

    public Instant getCommittedAt() {
        return delegate.getCommittedAt();
    }

    public Instant getCreatedAt() {
        return delegate.getAuthoredAt();
    }

    public String getMessage() {
        return delegate.getMessage();
    }

}
