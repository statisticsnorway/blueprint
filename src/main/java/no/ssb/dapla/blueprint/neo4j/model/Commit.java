package no.ssb.dapla.blueprint.neo4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@NodeEntity
public class Commit {

    @Relationship(type = "CREATES")
    private final Set<CreatedFile> creates = new LinkedHashSet<>();
    @Relationship(type = "UPDATES")
    private final Set<UpdatedFile> updates = new LinkedHashSet<>();
    @Relationship(type = "DELETES")
    private final Set<DeletedFile> deletes = new LinkedHashSet<>();
    @Relationship(type = "UNCHANGED")
    private final Set<UnchangedFile> unchanged = new LinkedHashSet<>();

    @Id
    private String id;

    private String authorName;
    private String authorEmail;
    private Instant authoredAt;
    private String committerName;
    private String committerEmail;
    private Instant committedAt;
    private String message;
    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
    private Repository repository;

    private Commit() {
    }

    public Commit(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public Instant getAuthoredAt() {
        return authoredAt;
    }

    public void setAuthoredAt(Instant authoredAt) {
        this.authoredAt = authoredAt;
    }

    public String getCommitterName() {
        return committerName;
    }

    public void setCommitterName(String committerName) {
        this.committerName = committerName;
    }

    public String getCommitterEmail() {
        return committerEmail;
    }

    public void setCommitterEmail(String committerEmail) {
        this.committerEmail = committerEmail;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    @JsonIgnore
    public Repository getRepository() {
        return repository;
    }

    public String getId() {
        return id;
    }

    @JsonIgnore
    public Set<CreatedFile> getCreates() {
        return Collections.unmodifiableSet(creates);
    }

    public void addCreate(String path, Notebook notebook) {
        addCreate(Path.of(path), notebook);
    }

    public void addCreate(Path path, Notebook notebook) {
        this.creates.add(new CreatedFile(this, path, notebook));
    }

    public Set<UpdatedFile> getUpdates() {
        return Collections.unmodifiableSet(updates);
    }

    public void addUpdate(Path path, Notebook notebook) {
        this.updates.add(new UpdatedFile(this, path, notebook));
    }

    public void addUpdate(String path, Notebook notebook) {
        addUpdate(Path.of(path), notebook);
    }

    public Set<DeletedFile> getDeletes() {
        return Collections.unmodifiableSet(deletes);
    }

    public void addDelete(String path, Notebook notebook) {
        addDelete(Path.of(path), notebook);
    }

    public void addDelete(Path path, Notebook notebook) {
        this.deletes.add(new DeletedFile(this, path, notebook));
    }

    public Set<UnchangedFile> getUnchanged() {
        return unchanged;
    }

    public void addUnchanged(String path, Notebook notebook) {
        addUnchanged(Path.of(path), notebook);
    }

    public void addUnchanged(Path path, Notebook notebook) {
        this.unchanged.add(new UnchangedFile(this, path, notebook));
    }
}
