package no.ssb.dapla.blueprint.notebook;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple model of the notebook metadata.
 * <p>
 * Used as a contract btw the parser and the service.
 */
public class Notebook {
    public String commitId;
    public String fileName;
    public String path;
    public List<String> inputs = new ArrayList<>();
    public List<String> outputs = new ArrayList<>();
}
