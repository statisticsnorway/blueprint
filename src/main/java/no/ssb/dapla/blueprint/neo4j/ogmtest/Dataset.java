package no.ssb.dapla.blueprint.neo4j.ogmtest;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.Objects;

@NodeEntity
public class Dataset {

    @Id
//    @GeneratedValue
//    private Long id;

    private String path;

//    @Relationship(type = "CONTAINS", direction = Relationship.INCOMING)
//    private Commit commit;

    private Dataset() {
    }

    public Dataset(String path, Commit commit) {
        this.path = Objects.requireNonNull(path);
//        this.commit = Objects.requireNonNull(commit);
    }

//    public Commit getCommit() {
//        return commit;
//    }
//
//    public void setCommit(Commit commit) {
//        this.commit = commit;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    private void setId(Long id) {
//        this.id = id;
//    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
