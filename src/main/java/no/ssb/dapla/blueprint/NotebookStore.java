package no.ssb.dapla.blueprint;

import no.ssb.dapla.blueprint.notebook.Dependency;
import no.ssb.dapla.blueprint.notebook.Notebook;
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
            MERGE (rev:GitRevision {
                commitId: $commitId,
                repositoryURL: $repositoryURL
            })
                        
            MERGE (rev)-[:MODIFIES]->(nb:Notebook {
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
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                var parameters = new HashMap<String, Object>();
                parameters.put("fileName", notebook.fileName);
                parameters.put("path", notebook.path);
                parameters.put("commitId", notebook.commitId);
                parameters.put("changed", notebook.changed);
                parameters.put("repositoryURL", notebook.repositoryURL);
                parameters.put("inputs", notebook.inputs);
                parameters.put("outputs", notebook.outputs);
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
                        MATCH (rev:GitRevision)-[:MODIFIES]->(nb:Notebook)-[t:CONSUMES|PRODUCES]->(ds:Dataset)
                        WHERE $commitId is null or rev.commitId = $commitId and $diff is null or nb.changed = $diff
                        RETURN rev, nb, ds, t
                        """, parameters);
                Map<Node, List<Record>> map = result.stream().collect(
                        Collectors.groupingBy(record -> record.get("nb").asNode()));

                ArrayList<Notebook> nbResult = new ArrayList<>();
                for (Node node : map.keySet()) {
                    Notebook notebook = new Notebook();
                    notebook.fileName = node.get("fileName").asString();
                    notebook.path = node.get("path").asString();
                    notebook.changed = node.get("changed").asBoolean();

                    notebook.repositoryURL = map.get(node).stream()
                            .map(record -> record.get("rev"))
                            .map(value -> value.get("repositoryURL").asString())
                            .findFirst()
                            .orElseThrow();

                    notebook.commitId = map.get(node).stream()
                            .map(record -> record.get("rev"))
                            .map(value -> value.get("commitId").asString())
                            .findFirst()
                            .orElseThrow();

                    notebook.outputs = map.get(node).stream()
                            .filter(record -> record.get("t").asRelationship().hasType("PRODUCES"))
                            .map(record -> record.get("ds").asNode().get("path").asString())
                            .collect(Collectors.toSet());

                    notebook.inputs = map.get(node).stream()
                            .filter(record -> record.get("t").asRelationship().hasType("CONSUMES"))
                            .map(record -> record.get("ds").asNode().get("path").asString())
                            .collect(Collectors.toSet());

                    nbResult.add(notebook);
                }
                return nbResult;
            });
        }
    }

    public List<Dependency> getDependencies(String revisionId) {
        return List.of();
    }

    public Notebook getNotebook(String revisionId, String notebookId) {
        try (Session session = driver.session()) {
            var parameters = Values.parameters(
                    "commitId", revisionId,
                    "notebookId", notebookId
            );
            var optionalRecord = session.readTransaction(tx -> {
                Result result = tx.run("""
                        MATCH (rev:GitRevision)-[:MODIFIES]->(nb:Notebook)
                        WHERE rev.commitId = $commitId and nb.id = notebookId
                        RETURN nb
                        """, parameters);
                return result.stream().findFirst();
            });

            return optionalRecord.map(record -> {
                var node = record.get("").asNode();
                Notebook notebook = new Notebook();
                notebook.fileName = node.get("fileName").asString();
                notebook.path = node.get("path").asString();
                notebook.changed = node.get("changed").asBoolean();
                return notebook;
            }).orElse(null);
        }
    }
}
