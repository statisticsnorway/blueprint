package no.ssb.dapla.blueprint.notebook;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple model of the notebook metadata.
 * <p>
 * Used as a contract btw the parser and the service.
 */
public class Notebook {

    public String repositoryURL;
    public String commitId;
    public String fileName;
    public String path;
    public Set<String> inputs = new HashSet<>();
    public Set<String> outputs = new HashSet<>();
}
