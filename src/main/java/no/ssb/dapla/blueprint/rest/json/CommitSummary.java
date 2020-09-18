package no.ssb.dapla.blueprint.rest.json;

public class CommitSummary {

    private final no.ssb.dapla.blueprint.neo4j.model.Commit delegate;

    public CommitSummary(no.ssb.dapla.blueprint.neo4j.model.Commit delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public String getAuthor() {
        return delegate.getAuthor();
    }

    public Object getCreatedAt() {
        return delegate.getCreatedAt();
    }

}
