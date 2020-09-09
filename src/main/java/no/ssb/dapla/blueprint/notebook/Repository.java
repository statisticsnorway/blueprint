package no.ssb.dapla.blueprint.notebook;

import org.eclipse.jgit.util.Hex;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Repository {

    private final URI uri;

    public Repository(String uri) {
        this.uri = URI.create(uri);
    }

    public Repository(URI uri) {
        this.uri = uri;
    }

    public static String computeHash(URI remote) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            var hash = digest.digest(remote.normalize().toASCIIString().getBytes());
            return Hex.toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URI getUri() {
        return uri;
    }

    public String getId() {
        return computeHash(getUri());
    }
}
