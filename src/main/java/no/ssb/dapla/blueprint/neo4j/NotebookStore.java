package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Dependency;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.util.*;

/**
 * TODO: Evaluate https://neo4j-contrib.github.io/cypher-dsl
 */
public class NotebookStore {

    private final Session session;

    public NotebookStore(SessionFactory factory) {
        this.session = Objects.requireNonNull(factory).openSession();
    }

    public NotebookStore(Session session) {
        this.session = Objects.requireNonNull(session);
    }

    public void addNotebook(Notebook notebook) {
        session.save(notebook);
    }

    public List<Notebook> getNotebooks() {
        return new ArrayList<>(session.loadAll(Notebook.class));
    }

    public List<Notebook> getNotebooks(String revisionId) {
        return getNotebooks(revisionId, false);
    }

    public List<Notebook> getNotebooks(String revisionId, Boolean diff) {
        var commit = session.load(Commit.class, revisionId);
        if (diff) {
            return new ArrayList<>(commit.getUpdates());
        } else {
            var updatesAndCreate = new ArrayList<Notebook>();
            updatesAndCreate.addAll(commit.getUpdates());
            updatesAndCreate.addAll(commit.getCreates());
            return updatesAndCreate;
        }
    }

    public List<Dependency> getDependencies(String revisionId) {
        return List.of();
    }

    public Notebook getNotebook(String revisionId, String blobId) {
        return session.queryForObject(Notebook.class, """
                TODO: TODO
                """, Map.of("commitId", revisionId, "blobId", blobId));
    }

    public Collection<Repository> getRepositories() {
        return session.loadAll(Repository.class, 0);
    }

    public Optional<Collection<Commit>> getCommits(String repositoryId) {
        var repository = session.load(Repository.class, repositoryId);
        return Optional.ofNullable(repository).map(Repository::getCommits);
    }

    public void saveRepository(Repository repository) {
        session.save(repository);
    }
}
