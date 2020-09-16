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

    public CommittedFile(Commit commit, Path path, Notebook notebook) {
        this.path = Objects.requireNonNull(path);
        this.commit = Objects.requireNonNull(commit);
        this.notebook = Objects.requireNonNull(notebook);
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
