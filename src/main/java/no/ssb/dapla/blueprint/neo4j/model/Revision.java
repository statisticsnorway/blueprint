package no.ssb.dapla.blueprint.neo4j.model;

public class Revision {

    private final String id;
    private Repository repository;

    public Revision(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
