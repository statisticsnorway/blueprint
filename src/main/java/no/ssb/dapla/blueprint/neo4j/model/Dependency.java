package no.ssb.dapla.blueprint.neo4j.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return producer.equals(that.producer) &&
                consumer.equals(that.consumer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producer, consumer);
    }
}
