package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.notebook.Notebook;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Processor that can add git related information to the notebooks.
 */
public class GitNotebookProcessor extends NotebookProcessor {

    private static final Logger log = LoggerFactory.getLogger(GitNotebookProcessor.class);

    private final Git git;
    private Map<String, DiffEntry> diffMap;

    public GitNotebookProcessor(ObjectMapper mapper, Git git) {
        super(mapper);
        this.git = Objects.requireNonNull(git);
    }

    @Override
    public Notebook process(Path path) throws IOException {
        var diffEntries = getDiffMap();
        var notebook = super.process(path);

        // Since we are only looking at the files that exists a match
        // here always means that the file was changed.
        notebook.changed = diffEntries.containsKey(notebook.fileName);
        return notebook;
    }

    private String getHead() throws IOException {
        ObjectId id = git.getRepository().resolve(Constants.HEAD);
        return id.getName();
    }

    private Map<String, DiffEntry> getDiffMap() throws IOException {
        if (this.diffMap == null) {
            this.diffMap = new HashMap<>();

            var commitId = getHead();
            CanonicalTreeParser currentTree = getTree(git, commitId + "^{tree}");
            CanonicalTreeParser previousTree = getTree(git, commitId + "~1^{tree}");
            if (previousTree != null) {
                var diffCommand = git.diff().setOldTree(previousTree).setNewTree(currentTree).setShowNameAndStatusOnly(true);
                try {
                    for (DiffEntry diffEntry : diffCommand.call()) {
                        this.diffMap.put(diffEntry.getNewPath(), diffEntry);
                    }
                } catch (GitAPIException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
        return Collections.unmodifiableMap(this.diffMap);
    }

    private CanonicalTreeParser getTree(Git git, String commitId) throws IOException {
        try (ObjectReader reader = git.getRepository().newObjectReader()) {
            var treeParser = new CanonicalTreeParser();
            var tree = git.getRepository().resolve(commitId);
            if (tree == null) {
                return null;
            } else {
                treeParser.reset(reader, tree);
                return treeParser;
            }
        }
    }
}
