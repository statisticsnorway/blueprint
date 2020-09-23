package no.ssb.dapla.blueprint.neo4j.model;

public class Dependency {

    private final Notebook producer;
    private final Notebook consumer;

    public Dependency(Notebook producer, Notebook consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public Notebook getProducer() {
        return producer;
    }

    public Notebook getConsumer() {
        return consumer;
    }
}
