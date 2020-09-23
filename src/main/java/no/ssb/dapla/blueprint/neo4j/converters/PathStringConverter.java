package no.ssb.dapla.blueprint.neo4j.converters;

import org.neo4j.ogm.typeconversion.AttributeConverter;

import java.nio.file.Path;

public class PathStringConverter implements AttributeConverter<Path, String> {

    @Override
    public String toGraphProperty(Path value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    @Override
    public Path toEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return Path.of(value);
    }
}
