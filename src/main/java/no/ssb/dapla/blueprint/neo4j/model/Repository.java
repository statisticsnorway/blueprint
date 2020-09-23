package no.ssb.dapla.blueprint.neo4j.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.ssb.dapla.blueprint.neo4j.GitStore;
import no.ssb.dapla.blueprint.neo4j.converters.URIStringConverter;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

@NodeEntity
public class Repository {

    @Relationship(type = "CONTAINS")
    final Set<Commit> commits = new LinkedHashSet<>();
    @Id
    private String id;
    @Convert(URIStringConverter.class)
    private URI uri;

    private Repository() {
    }

    public Repository(String uri) {
        this(URI.create(uri));
    }

    public Repository(URI uri) {
        this.uri = uri;
        this.id = GitStore.computeHash(uri);
    }

    @JsonIgnore
    public Set<Commit> getCommits() {
        return commits;
    }

    public String getId() {
        return id;
    }

    public void addCommit(Commit commit) {
        commits.add(commit);
    }

    public URI getUri() {
        return uri;
    }

}
