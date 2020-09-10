package no.ssb.dapla.blueprint.neo4j;

import no.ssb.dapla.blueprint.neo4j.ogmtest.Commit;
import no.ssb.dapla.blueprint.neo4j.ogmtest.Dataset;
import no.ssb.dapla.blueprint.neo4j.ogmtest.Notebook;
import no.ssb.dapla.blueprint.neo4j.ogmtest.Repository;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;

public class NeoOgmTest {

    @Test
    void name() {

        Configuration configuration = new Configuration.Builder()
                .uri("bolt://neo4j:password@localhost")
                .connectionPoolSize(150)
                .build();
        //Driver driver = GraphDatabase.driver("bolt://localhost");
        SessionFactory factory = new SessionFactory(configuration, Commit.class.getPackageName());

        Session session = factory.openSession();

        Commit commit1 = new Commit("sha1");
        commit1.setAuthor("Hadrien");
        commit1.setCreatedAt(LocalDateTime.of(2020, 1, 1, 1, 1).toInstant(ZoneOffset.UTC));


        Commit commit2 = new Commit("sha2");
        commit2.setAuthor("Arild");
        commit2.setCreatedAt(LocalDateTime.of(2021, 1, 1, 1, 1).toInstant(ZoneOffset.UTC));

        Commit commit3 = new Commit("sha2");
        commit3.setAuthor("Bjørn-Andre");
        commit3.setCreatedAt(LocalDateTime.of(2021, 10, 1, 1, 1).toInstant(ZoneOffset.UTC));

        var dsStart = new Dataset("/start", commit1);
        Notebook notebook1 = new Notebook("notebook1");
        notebook1.setPath("/foo/bar/1.ipynb");
        notebook1.setConsumes(Set.of(dsStart));
        var dsA = new Dataset("/a", commit1);
        var dsB = new Dataset("/b", commit1);
        notebook1.setProduces(Set.of(dsA, dsB));


        Notebook notebook2 = new Notebook("notebook2");
        notebook2.setPath("/foo/bar/2.ipynb");
        notebook2.setConsumes(Set.of(dsA, dsB));
        var dsC = new Dataset("/c", commit1);
        var dsD = new Dataset("/d", commit1);
        notebook2.setProduces(Set.of(dsC, dsD));

        var notebook3 = new Notebook("notebook3");
        notebook3.setPath("/foo/bar/3.ipynb");
        notebook3.setConsumes(Set.of(dsC));
        var dsE = new Dataset("/e", commit1);
        notebook3.setProduces(Set.of(dsE));

        var notebook4 = new Notebook("notebook4");
        notebook4.setPath("/foo/bar/4.ipynb");
        notebook4.setConsumes(Set.of(dsD, dsE));
        var dsEnd = new Dataset("/end", commit1);
        notebook4.setProduces(Set.of(dsEnd));

        commit1.setCreates(Set.of(notebook1, notebook2, notebook3, notebook4));

        Repository repository = new Repository("http://example.com/test");
        repository.setCommits(Set.of(commit1));

        session.purgeDatabase();
        session.save(repository);

        Collection<Repository> all = session.loadAll(Repository.class);

        System.out.println(all);
    }
}
