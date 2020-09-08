package no.ssb.dapla.blueprint.notebook;

public class Repository {

    public String getUri() {
        return uri;
    }

    private final String uri;

    public Repository(String uri) {
        this.uri = uri;
    }
}
