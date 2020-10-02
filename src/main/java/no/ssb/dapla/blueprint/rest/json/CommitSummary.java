package no.ssb.dapla.blueprint.rest.json;

import no.ssb.dapla.blueprint.neo4j.model.Commit;

import java.time.Instant;

public class CommitSummary {

    protected final Commit delegate;

    public CommitSummary(Commit delegate) {
        this.delegate = delegate;
    }

    public String getId() {
        return delegate.getId();
    }

    public Person getAuthor() {
        return new Person(delegate.getAuthorName(), delegate.getAuthorEmail());
    }

    public Person getCommitter() {
        return new Person(delegate.getCommitterName(), delegate.getCommitterEmail());
    }

    public Instant getAuthoredAt() {
        return delegate.getAuthoredAt();
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

    public static class Person {
        private final String name;
        private final String email;

        public Person(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
