package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.CommittedFile;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
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

    public void purgeDatabase() {
        session.purgeDatabase();
    }

    @Deprecated
    public List<Notebook> getNotebooks() {
        return new ArrayList<>(session.loadAll(Notebook.class));
    }

    @Deprecated
    public List<Notebook> getNotebooks(String revisionId) {
        return getNotebooks(revisionId, false);
    }

    @Deprecated
    public List<Notebook> getNotebooks(String revisionId, Boolean diff) {
        var commit = session.load(Commit.class, revisionId);
        Set<CommittedFile> files = new HashSet<>();
        files.addAll(commit.getCreates());
        files.addAll(commit.getUpdates());
        files.addAll(commit.getUnchanged());
        return files.stream().map(CommittedFile::getNotebook).collect(Collectors.toList());
    }

    @Deprecated
    public Commit getCommit(String commitId) {
        return session.load(Commit.class, commitId);
    }

    @Deprecated
    public Notebook getNotebook(String revisionId, String blobId) {
        return session.queryForObject(Notebook.class, """
                TODO: TODO
                """, Map.of("commitId", revisionId, "blobId", blobId));
    }

    public Collection<Repository> getRepositories() {
        return session.loadAll(Repository.class, 0);
    }

    public Optional<Collection<Commit>> getCommits(String repositoryId) {
        Repository repository = session.load(Repository.class, repositoryId, 0);
        if (repository == null) {
            return Optional.empty();
        } else {
            Iterable<Commit> commits = session.query(
                    Commit.class, """
                            OPTIONAL MATCH (repository:Repository {id: $repositoryId})-[c:CONTAINS]->(commit:Commit)
                            RETURN repository, c, commit ORDER BY commit.createdAt DESC
                            """,
                    Map.of("repositoryId", repositoryId)
            );
            ArrayList<Commit> result = new ArrayList<>();
            commits.forEach(result::add);
            return Optional.of(result);
        }
    }

    public Commit findOrCreateCommit(String id) {
        var commit = session.load(Commit.class, id, 0);
        if (commit == null) {
            commit = new Commit(id);
            session.save(commit);
        }
        return commit;
    }

    public Repository findOrCreateRepository(URI uri) {
        // TODO: Maybe use the URI as id and use a field with index for the id?
        var id = new Repository(uri).getId();
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

    /**
     * Return a commit with all the notebook and datasets.
     */
    public Optional<Commit> getCommit(String repositoryId, String commitId) {
        Repository repository = session.load(Repository.class, repositoryId, 0);
        if (repository == null) {
            return Optional.empty();
        } else {
            Commit commit = session.queryForObject(
                    Commit.class, """
                            MATCH (repository:Repository {id: $repositoryId})-[r:CONTAINS]->(commit:Commit {id : $commitId})
                            MATCH (commit)-[file]->(notebook:Notebook)
                            OPTIONAL MATCH (notebook)-[ds]->(dataset:Dataset)
                            RETURN repository, r, commit, file, notebook, ds, dataset ORDER BY file.path ASC
                             """,
                    Map.of("repositoryId", repositoryId, "commitId", commitId)
            );
            return Optional.ofNullable(commit);
        }
    }

    /**
     * Returns a commit a spanning tree of the notebook graph.
     */
    public Optional<Commit> getDependencies(String repositoryId, String commitId) {
        Commit commit = session.queryForObject(Commit.class, """
                MATCH (repository:Repository {id: $repositoryId})-[rc:CONTAINS]->(commit:Commit {id: $commitId})
                MATCH (commit)-[:CREATES|UPDATES|UNCHANGED]->(nb:Notebook)
                CALL apoc.path.spanningTree(nb, {
                  relationshipFilter: "PRODUCES>|<CONSUMES",
                  minLevel: 1,
                  maxLevel: -1
                })
                YIELD path
                WITH nodes(path) as nbs, repository, rc, commit
                UNWIND nbs as notebook
                MATCH (commit:Commit)-[file:CREATES|UPDATES|UNCHANGED]->(notebook)
                MATCH (notebook)-[nd:PRODUCES|CONSUMES]-(dataset:Dataset)
                RETURN repository, rc, commit, file, notebook, nd, dataset            
                """, Map.of("repositoryId", repositoryId, "commitId", commitId)
        );
        return Optional.ofNullable(commit);
    }

    /**
     * Returns a commit a spanning tree of a particular notebook.
     */
    public Optional<Commit> getDependencies(String repositoryId, String commitId, String notebookId) {
        Commit commit = session.queryForObject(Commit.class, """
                MATCH (repository:Repository {id: $repositoryId})-[rc:CONTAINS]->(commit:Commit {id: $commitId})
                MATCH (commit)-[:CREATES|UPDATES|UNCHANGED]->(nb:Notebook {blobId: $notebookId})
                CALL apoc.path.spanningTree(nb, {
                  relationshipFilter: "PRODUCES>|<CONSUMES",
                  minLevel: 1,
                  maxLevel: -1
                })
                YIELD path
                WITH nodes(path) as nbs, repository, rc, commit
                UNWIND nbs as notebook
                MATCH (commit:Commit)-[file:CREATES|UPDATES|UNCHANGED]->(notebook)
                MATCH (notebook)-[nd:PRODUCES|CONSUMES]-(dataset:Dataset)
                RETURN repository, rc, commit, file, notebook, nd, dataset            
                """, Map.of("repositoryId", repositoryId, "commitId", commitId, "notebookId", notebookId)
        );
        return Optional.ofNullable(commit);
    }
}
