package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.BlueprintService;
import no.ssb.dapla.blueprint.notebook.Notebook;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;

import java.util.HashMap;
import java.util.Objects;

public class Neo4jOutput implements Output {

    private static final Query INSERT_NOTEBOOK = new Query("""
            
            MERGE (rev:GitRevision {commitId:$commitId})
            
            MERGE (rev)-[:MODIFIES]-(nb:Notebook {
                fileName: $fileName,
                path: $path
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

    // private final BlueprintService blueprintService;
    private final Driver driver;

    //public Neo4jOutput(BlueprintService blueprintService) {
    //    this.blueprintService = Objects.requireNonNull(blueprintService);
    //}

    public Neo4jOutput(Driver driver) {
    //    this.blueprintService = new BlueprintService(null, Objects.requireNonNull(driver));
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public void output(Notebook notebook) {
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
