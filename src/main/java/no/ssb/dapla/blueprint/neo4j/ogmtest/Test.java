package no.ssb.dapla.blueprint.neo4j.ogmtest;

import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class Test {

    private final SessionFactory factory;

    public Test() {
        Configuration configuration = new Configuration.Builder()
                .uri("bolt://neo4j:password@localhost")
                .connectionPoolSize(150)
                .build();
        //Driver driver = GraphDatabase.driver("bolt://localhost");
        factory = new SessionFactory(configuration, Commit.class.getPackageName());
    }

    public void addNotebookWithOgm(Notebook oldNotebook) {
        Session session = factory.openSession();

        var repository = new no.ssb.dapla.blueprint.neo4j.ogmtest.Repository(oldNotebook.getRevision().getRepository().getUri());

        var commit = new Commit(oldNotebook.getRevision().getId());
        commit.setCreatedAt(Instant.now());
        commit.setAuthor("Hadrien");
        commit.setRepository(repository);
        //repository.setCommits(Set.of(commit));

        var notebook = new no.ssb.dapla.blueprint.neo4j.ogmtest.Notebook(oldNotebook.getBlobId());
        notebook.setPath(oldNotebook.getPath().toString());

        if (oldNotebook.isChanged()) {
            notebook.setUpdatedIn(Set.of(commit));
        } else {
            notebook.setCreatedIn(commit);
        }

        Set<Dataset> inputs = new HashSet<>();
        for (String input : oldNotebook.getInputs()) {
            inputs.add(new Dataset(input, commit));
        }

        Set<Dataset> outputs = new HashSet<>();
        for (String output : oldNotebook.getOutputs()) {
            outputs.add(new Dataset(output, commit));
        }

        notebook.setConsumes(inputs);
        notebook.setProduces(outputs);

        session.save(notebook);

    }
}
