package no.ssb.dapla.blueprint.neo4j.model;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.time.Instant;
import java.util.*;

@NodeEntity
public class Commit {

    @Id
    private String id;

    private String author;

    private Instant createdAt;

    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
    private Repository repository;

    @Relationship(type = "CREATES")
    private final Set<Notebook> creates = new HashSet<>();

    @Relationship(type = "UPDATES")
    private final Set<Notebook> updates = new HashSet<>();

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
        repository.addCommit(this);
    }

    public String getId() {
        return id;
    }

    public Set<Notebook> getCreates() {
        return creates;
    }

    public void addCreate(Collection<Notebook> notebooks) {
        this.creates.addAll(notebooks);
    }

    public void addCreate(Notebook... notebooks) {
        addCreate(Arrays.asList(notebooks));
    }

    public void addUpdate(Collection<Notebook> notebooks) {
        this.updates.addAll(notebooks);
    }

    public void addUpdate(Notebook... notebooks) {
        addUpdate(Arrays.asList(notebooks));
    }

    public Set<Notebook> getUpdates() {
        return updates;
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
