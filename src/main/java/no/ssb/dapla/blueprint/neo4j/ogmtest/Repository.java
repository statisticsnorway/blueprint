package no.ssb.dapla.blueprint.neo4j.ogmtest;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

@NodeEntity
public class Repository {

    @Id
    private String uri;

    @Relationship(type = "CONTAINS")
    private Set<Commit> commits;

    public Repository(URI uri) {
        this.uri = uri.normalize().toASCIIString();
    }

    public Repository(String uri) {
        this.uri = Objects.requireNonNull(uri);
    }

    public Set<Commit> getCommits() {
        return commits;
    }

    public void setCommits(Set<Commit> commits) {
        this.commits = commits;
    }

    public String getUri() {
        return uri;
    }

}
