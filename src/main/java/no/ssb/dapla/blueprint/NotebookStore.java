package no.ssb.dapla.blueprint;

import no.ssb.dapla.blueprint.notebook.Notebook;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;

import java.util.HashMap;
import java.util.Objects;

public class NotebookStore {

    private static final Query INSERT_NOTEBOOK = new Query("""
            MERGE (rev:GitRevision {commitId:$commitId})
                        
            MERGE (rev)-[:MODIFIES]->(nb:Notebook {
                fileName: $fileName,
                path: $path
            })
                        
            WITH nb
            UNWIND $inputs as input 
                MERGE (ds:Dataset {path: input, commitId:$commitId})
                MERGE (nb)-[:CONSUMES]-(ds)

            WITH nb
            UNWIND $outputs as output
              MERGE (ds:Dataset {path: output, commitId:$commitId})
              MERGE (nb)-[:PRODUCES]-(ds)
                                            
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
                parameters.put("inputs", notebook.inputs);
                parameters.put("outputs", notebook.outputs);
                return tx.run(INSERT_NOTEBOOK.withParameters(parameters));
            });
        }
    }
}
