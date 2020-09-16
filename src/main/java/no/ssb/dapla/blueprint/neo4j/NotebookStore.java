package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.neo4j.model.*;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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
        // TODO: Wrap in representation.
        Set<CommittedFile> files = new HashSet<>();
        files.addAll(commit.getCreates());
        files.addAll(commit.getUpdates());
        return files.stream().map(CommittedFile::getNotebook).collect(Collectors.toList());
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

    public Commit findOrCreateCommit(String id) {
        var commit = session.load(Commit.class, id, 0);
        if (commit == null) {
            commit = new Commit(id);
            session.save(commit);
        }
        return commit;
    }

    public Repository findOrCreateRepository(String id, URI uri) {
        var commit = session.load(Repository.class, id, 0);
        if (commit == null) {
            commit = new Repository(uri);
            session.save(commit);
        }
        return commit;
    }

    public void saveRepository(Repository repository) {
        session.save(repository);
    }

    public void saveCommit(Commit commit) {
        session.save(commit);
    }
}
