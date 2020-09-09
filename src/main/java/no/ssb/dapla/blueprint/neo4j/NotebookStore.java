package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.neo4j.model.Dependency;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import no.ssb.dapla.blueprint.neo4j.model.Revision;
import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO: Evaluate https://neo4j-contrib.github.io/cypher-dsl
 */
public class NotebookStore {

    private static final Query INSERT_NOTEBOOK = new Query("""
            MERGE (repo:Repository {
                repositoryId: $repositoryId,
                repositoryURI: $repositoryURI
            })
                        
            MERGE (repo)-[:CONTAINS]->(rev:GitRevision {
                commitId: $commitId
            })
                        
            MERGE (rev)-[:MODIFIES]->(nb:Notebook {
                blobId: $blobId,
                fileName: $fileName,
                path: $path,
                changed: $changed                
            })
                        
            WITH nb
            UNWIND $inputs as input 
                MERGE (ds:Dataset {path: input, commitId:$commitId})
                MERGE (nb)-[:CONSUMES]->(ds)

            WITH nb
            UNWIND $outputs as output
              MERGE (ds:Dataset {path: output, commitId:$commitId})
              MERGE (nb)-[:PRODUCES]->(ds)
                                            
            """);

    private final Driver driver;

    public NotebookStore(Driver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    public void addNotebook(Notebook notebook) {

        Revision revision = notebook.getRevision();
        Repository repository = revision.getRepository();

        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                var parameters = new HashMap<String, Object>();
                parameters.put("repositoryId", repository.getId());
                parameters.put("repositoryURI", repository.getUri().toASCIIString());

                parameters.put("commitId", revision.getId());

                // Note the to string here. Path implement Iterable<Iterable<Path>> so neo4j fails with
                // StackOverflowError.
                parameters.put("fileName", notebook.getFileName().toString());
                parameters.put("path", notebook.getPath().toString());

                parameters.put("changed", notebook.isChanged());
                parameters.put("inputs", notebook.getInputs());
                parameters.put("outputs", notebook.getOutputs());
                parameters.put("blobId", notebook.getBlobId());
                return tx.run(INSERT_NOTEBOOK.withParameters(parameters));
            });
        }
    }

    public List<Notebook> getNotebooks() {
        return getNotebooks(null, null);
    }

    public List<Notebook> getNotebooks(String revisionId) {
        return getNotebooks(revisionId, null);
    }

    public List<Notebook> getNotebooks(String revisionId, Boolean diff) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                var parameters = Values.parameters(
                        "commitId", revisionId,
                        "diff", diff
                );
                Result result = tx.run("""
                        MATCH (repo:Repository)-[:CONTAINS]->(rev:GitRevision)
                        MATCH (rev)-[:MODIFIES]->(nb:Notebook)-[t:CONSUMES|PRODUCES]->(ds:Dataset)
                        WHERE $commitId is null or rev.commitId = $commitId and $diff is null or nb.changed = $diff
                        RETURN repo, rev, nb, t, ds
                        """, parameters);
                Map<Node, List<Record>> map = result.stream().collect(
                        Collectors.groupingBy(record -> record.get("nb").asNode()));

                ArrayList<Notebook> nbResult = new ArrayList<>();
                for (Node node : map.keySet()) {

                    var notebook = nodeToNotebook(node);

                    Repository repository = map.get(node).stream().map(record -> record.get("repo").asNode())
                            .findFirst()
                            .map(repo -> new Repository(
                                    repo.get("repositoryURI").asString()
                            )).orElseThrow();

                    Revision revision = map.get(node).stream().map(record -> record.get("rev").asNode())
                            .findFirst()
                            .map(repo -> new Revision(
                                    repo.get("commitId").asString()
                            )).orElseThrow();

                    revision.setRepository(repository);
                    notebook.setRevision(revision);

                    notebook.setOutputs(map.get(node).stream()
                            .filter(record -> record.get("t").asRelationship().hasType("PRODUCES"))
                            .map(record -> record.get("ds").asNode().get("path").asString())
                            .collect(Collectors.toSet())
                    );

                    notebook.setInputs(map.get(node).stream()
                            .filter(record -> record.get("t").asRelationship().hasType("CONSUMES"))
                            .map(record -> record.get("ds").asNode().get("path").asString())
                            .collect(Collectors.toSet())
                    );

                    nbResult.add(notebook);
                }
                return nbResult;
            });
        }
    }

    public List<Dependency> getDependencies(String revisionId) {
        return List.of();
    }

    public Notebook getNotebook(String revisionId, String blobId) {
        try (Session session = driver.session()) {
            var parameters = Values.parameters(
                    "commitId", revisionId,
                    "blobId", blobId
            );
            var optionalRecord = session.readTransaction(tx -> {
                Result result = tx.run("""
                        MATCH (rev:GitRevision)-[:MODIFIES]->(nb:Notebook)
                        WHERE rev.commitId = $commitId and nb.blobId = $blobId
                        RETURN nb
                        """, parameters);
                return result.stream().findFirst();
            });

            return optionalRecord.map(record -> record.get("nb").asNode())
                    .map(this::nodeToNotebook).orElse(null);
        }
    }

    private Notebook nodeToNotebook(Node node) {
        Notebook notebook = new Notebook();
        notebook.setPath(node.get("path").asString());
        notebook.setChanged(node.get("changed").asBoolean());
        notebook.setBlobId(node.get("blobId").asString());
        return notebook;
    }
}
