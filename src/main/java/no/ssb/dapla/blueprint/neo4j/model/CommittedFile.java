package no.ssb.dapla.blueprint.neo4j.model;

import no.ssb.dapla.blueprint.neo4j.converters.PathStringConverter;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.nio.file.Path;
import java.util.Objects;


public class CommittedFile {
    @Id
    @GeneratedValue
    private Long id;

    @Convert(PathStringConverter.class)
    private Path path;

    @StartNode
    private Commit commit;

    @EndNode
    private Notebook notebook;

    CommittedFile() {
    }

    public CommittedFile(Commit commit, Path path, Notebook notebook) {
        this.path = Objects.requireNonNull(path);
        this.commit = Objects.requireNonNull(commit);
        this.notebook = Objects.requireNonNull(notebook);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommittedFile that = (CommittedFile) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public Notebook getNotebook() {
        return notebook;
    }

    public Commit getCommit() {
        return commit;
    }

    public Path getPath() {
        return path;
    }

    public Long getId() {
        return id;
    }
}
