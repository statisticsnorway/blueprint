package no.ssb.dapla.blueprint.neo4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NodeEntity
public class Repository {

    @Id
    private String uri;

    @JsonIgnore
    public Set<Commit> getCommits() {
        return commits;
    }

    @Relationship(type = "CONTAINS")
    final Set<Commit> commits = new HashSet<>();

    private Repository() {
    }

    public Repository(URI uri) {
        this.uri = uri.normalize().toASCIIString();
    }

    public Repository(String uri) {
        this.uri = Objects.requireNonNull(uri);
    }

    public void addCommit(Commit commit) {
        commits.add(commit);
    }

    public String getUri() {
        return uri;
    }

}
