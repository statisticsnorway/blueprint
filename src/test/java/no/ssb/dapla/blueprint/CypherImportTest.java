package no.ssb.dapla.blueprint;

import no.ssb.dapla.blueprint.notebook.Notebook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

@Deprecated
@ExtendWith(EmbeddedNeo4jExtension.class)
public class CypherImportTest {

    private static final Logger LOG = LoggerFactory.getLogger(CypherImportTest.class);

    static {
        BlueprintApplication.initLogging();
    }

    @Test
    public void thatBulkImportOfNotebooksWorks(Driver driver) {
        final String commitId = "a32bf418ea";

        List<Notebook> notebookList = createSkattFregFamNotebooks(commitId);

        List<Object> notebooks = new ArrayList<>();
        for (Notebook notebook : notebookList) {
            List<Object> nb = new ArrayList<>();
            nb.add(notebook.path + notebook.fileName);
            nb.add(notebook.inputs);
            nb.add(notebook.outputs);
            notebooks.add(nb);
        }
        List<Object> batch = List.of(commitId, notebooks);

        try (Session session = driver.session()) {
            String summary = session.writeTransaction(tx -> {
                Result deletePreviousGitRevisionResult = tx.run("" +
                                "MATCH (n:GitRevision {revision: $rev})\n" +
                                "OPTIONAL MATCH (n)--(nb:Notebook)\n" +
                                "OPTIONAL MATCH (nb)--(ds:Dataset)\n" +
                                "DETACH DELETE n, nb, ds",
                        parameters("rev", commitId));
                SummaryCounters deletePreviousGitRevisionCounters = deletePreviousGitRevisionResult.consume().counters();
                int nodesDeleted = deletePreviousGitRevisionCounters.nodesDeleted();
                int relationshipsDeleted = deletePreviousGitRevisionCounters.relationshipsDeleted();

                Result createGitRevisionResult = tx.run("" +
                                "CREATE (r:GitRevision) SET r.revision = $batch[0]\n" +
                                "WITH r, $batch[1] as notebooks\n" +
                                "UNWIND notebooks AS notebook\n" +
                                "CREATE (r)<-[:GitRev]-(nb:Notebook {path: notebook[0]})\n" +
                                "FOREACH (input IN notebook[1] |\n" +
                                "  MERGE (r)<-[:TmpGitRev]-(ds:Dataset {path: input})\n" +
                                "  MERGE (nb)-[:Input]->(ds)\n" +
                                ")\n" +
                                "FOREACH (output IN notebook[2] |\n" +
                                "  MERGE (r)<-[:TmpGitRev]-(ds:Dataset {path: output})\n" +
                                "  MERGE (nb)-[:Output]->(ds)\n" +
                                ")",
                        parameters("batch", batch));
                SummaryCounters createGitRevisionCounters = createGitRevisionResult.consume().counters();
                int nodesCreated = createGitRevisionCounters.nodesCreated();
                int relationshipsCreated = createGitRevisionCounters.relationshipsCreated();

                Result deleteTempGitRevRelationshipsResult = tx.run("" +
                                "MATCH (:Dataset)-[l:TmpGitRev]->(:GitRevision{revision: $rev}) DELETE l;",
                        parameters("rev", commitId));
                SummaryCounters deleteTempGitRevRelationshipsCounters = deleteTempGitRevRelationshipsResult.consume().counters();
                int tmpRelationshipsDeleted = deleteTempGitRevRelationshipsCounters.relationshipsDeleted();

                return String.format("Deleted all data of existing git-revision '%s': Nodes: %d, Relationships: %d%n" +
                                "Created new data for git-revision '%s': Nodes: %d, Relationships: %d%n"
                        , commitId, nodesDeleted, relationshipsDeleted,
                        commitId, nodesCreated, relationshipsCreated - tmpRelationshipsDeleted);
            });

            System.out.printf("%s%n", summary);
        }
    }

    private List<Notebook> createSkattFregFamNotebooks(String commitId) {
        List<Notebook> notebookList = new ArrayList<>();
        {
            Notebook notebook = new Notebook();
            notebook.commitId = commitId;
            notebook.path = "/";
            notebook.fileName = "skatt.ipynb";
            notebook.inputs.add("/skatt/r");
            notebook.outputs.add("/skatt/k");
            notebookList.add(notebook);
        }
        {
            Notebook notebook = new Notebook();
            notebook.commitId = commitId;
            notebook.path = "/";
            notebook.fileName = "freg.ipynb";
            notebook.inputs.add("/freg/r");
            notebook.outputs.add("/freg/k");
            notebookList.add(notebook);
        }
        {
            Notebook notebook = new Notebook();
            notebook.commitId = commitId;
            notebook.path = "/";
            notebook.fileName = "familie.ipynb";
            notebook.inputs.add("/skatt/k");
            notebook.inputs.add("/freg/k");
            notebook.outputs.add("/fam");
            notebookList.add(notebook);
        }
        return notebookList;
    }
}
