package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.EmbeddedNeo4jExtension;
import no.ssb.dapla.blueprint.neo4j.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.ogm.session.SessionFactory;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EmbeddedNeo4jExtension.class)
class NotebookStoreTest {

    private static final Set<Dataset> DS_ONE = Set.of(
            new Dataset("/ds/one/one"),
            new Dataset("/ds/one/two"),
            new Dataset("/ds/one/three"),
            new Dataset("/ds/one/four")
    );

    private static final Set<Dataset> DS_TWO = Set.of(
            new Dataset("/ds/two/one"),
            new Dataset("/ds/two/two"),
            new Dataset("/ds/two/three"),
            new Dataset("/ds/two/four")
    );

    private static final Set<Dataset> DS_THREE = Set.of(
            new Dataset("/ds/three/one"),
            new Dataset("/ds/three/two"),
            new Dataset("/ds/three/three"),
            new Dataset("/ds/three/four")
    );

    private NotebookStore store;

    @BeforeEach
    void setUp(SessionFactory factory) {
        store = new NotebookStore(factory);
    }

    @Test
    void testInsertNotebook() {

        Repository repository = new Repository(URI.create("http://example.com/git/repo"));
        Commit commit = new Commit("commitId");

        commit.setAuthorName("Hadrien");
        commit.setAuthoredAt(Instant.ofEpochMilli(0));
        commit.setAuthorEmail("hadrien@ssb.no");

        commit.setCommitterName("Arild");
        commit.setCommittedAt(Instant.ofEpochMilli(10));
        commit.setCommitterEmail("arild@ssb.no");

        Notebook notebook1 = new Notebook("notebookId");
        Notebook notebook2 = new Notebook("notebookId");

        commit.addCreate("foo/bar", notebook1);
        commit.addCreate("bar/foo", notebook2);
        commit.setMessage("commit message");

        repository.addCommit(commit);

        store.saveRepository(repository);

        Optional<Commit> persistedCommit = store.getCommit(repository.getId(), commit.getId());
        assertThat(persistedCommit).usingFieldByFieldValueComparator()
                .contains(commit);
    }

    @Test
    void testInsertNotebooksWithMatchingInputOutput() {

        Repository repository = new Repository(URI.create("http://example.com/git/repo"));
        Commit commit = new Commit("commitId");

        commit.setAuthorName("Hadrien");
        commit.setAuthoredAt(Instant.ofEpochMilli(0));
        commit.setAuthorEmail("hadrien@ssb.no");

        commit.setCommitterName("Arild");
        commit.setCommittedAt(Instant.ofEpochMilli(10));
        commit.setCommitterEmail("arild@ssb.no");

        Notebook notebook1 = new Notebook("notebookId");
        notebook1.addInputs(DS_ONE);
        notebook1.addOutputs(DS_TWO);

        Notebook notebook2 = new Notebook("notebookId");
        notebook2.addInputs(DS_TWO);
        notebook2.addOutputs(DS_THREE);

        commit.addUnchanged("/bar/foo", notebook1);
        commit.addCreate("/foo/bar", notebook2);

        repository.addCommit(commit);
        store.saveRepository(repository);

        Optional<Commit> persistedCommit = store.getCommit(repository.getId(), commit.getId());
        assertThat(persistedCommit).isNotEmpty();
        assertThat(persistedCommit.get().getUnchanged()).extracting(CommittedFile::getNotebook)
                .contains(notebook1);
        assertThat(persistedCommit.get().getCreates()).extracting(CommittedFile::getNotebook)
                .contains(notebook2);

    }
}