package no.ssb.dapla.blueprint.neo4j.converters;

import org.neo4j.ogm.typeconversion.AttributeConverter;

import java.net.URI;

public class URIStringConverter implements AttributeConverter<URI, String> {
    @Override
    public String toGraphProperty(URI uri) {
        return uri.normalize().toASCIIString();
    }

    @Override
    public URI toEntityAttribute(String s) {
        return URI.create(s);
    }
}
