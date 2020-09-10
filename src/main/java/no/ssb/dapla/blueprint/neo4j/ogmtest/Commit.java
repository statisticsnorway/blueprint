package no.ssb.dapla.blueprint.neo4j.ogmtest;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@NodeEntity
public class Commit {

    @Id
    private String id;
    private String author;
    private Instant createdAt;

    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
    private Repository repository;

    @Relationship(type = "CREATES")
    private Set<Notebook> creates;

    @Relationship(type = "UPDATES")
    private Set<Notebook> updates;

    private Commit() {
    }

    public Commit(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public String getId() {
        return id;
    }

    public Set<Notebook> getCreates() {
        return creates;
    }

    public void setCreates(Set<Notebook> creates) {
        this.creates = creates;
    }

    public Set<Notebook> getUpdates() {
        return updates;
    }

    public void setUpdates(Set<Notebook> updates) {
        this.updates = updates;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

}
