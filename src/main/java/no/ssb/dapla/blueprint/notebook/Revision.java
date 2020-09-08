package no.ssb.dapla.blueprint.notebook;

public class Revision {

    public String getSha() {
        return sha;
    }

    private final String sha;

    public Revision(String sha) {
        this.sha = sha;
    }
}
