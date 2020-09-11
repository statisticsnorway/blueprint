package no.ssb.dapla.blueprint.neo4j.model;

import no.ssb.dapla.blueprint.neo4j.converters.PathStringConverter;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

import java.nio.file.Path;
import java.util.Objects;

@NodeEntity
public class Dataset {

    @Id
    @Convert(PathStringConverter.class)
    private Path path;


    private Dataset() {
    }

    public Dataset(String path) {
        this.path = Path.of(Objects.requireNonNull(path));
    }

    public Dataset(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    public Path getPath() {
        return path;
    }

    private void setPath(Path path) {
        this.path = path;
    }
}
