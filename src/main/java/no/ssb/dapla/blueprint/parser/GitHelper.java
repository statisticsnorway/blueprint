package no.ssb.dapla.blueprint.parser;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper around a repository to get diff info of a commit.
 */
public class GitHelper {

    private final Repository repository;

    public GitHelper(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public String getObjectId(String revSpec, Path filePath) throws IOException {
        try (var reader = repository.newObjectReader()) {
            var walk = new RevWalk(reader);
            var commitId = repository.resolve(revSpec);
            var commit = walk.parseCommit(commitId);
            var tree = commit.getTree();
            var treeWalk = TreeWalk.forPath(reader, filePath.toString(), tree);
            if (treeWalk == null) {
                throw new IOException(String.format("could not find file with path %s in commit %s", filePath, revSpec));
            } else {
                return treeWalk.getObjectId(0).getName();
            }
        }
    }

    public String getHead() throws IOException {
        ObjectId id = repository.resolve(Constants.HEAD);
        return id.getName();
    }

    public Map<String, DiffEntry> getDiffMap(String commitId) throws IOException {
        Map<String, DiffEntry> diffMap = new HashMap<>();
        CanonicalTreeParser currentTree = getTree(commitId + "^{tree}");
        CanonicalTreeParser previousTree = getTree(commitId + "~1^{tree}");
        if (previousTree != null) {
            var diffCommand = Git.wrap(repository)
                    .diff()
                    .setOldTree(previousTree)
                    .setNewTree(currentTree)
                    .setShowNameAndStatusOnly(true);
            try {
                for (DiffEntry diffEntry : diffCommand.call()) {
                    diffMap.put(diffEntry.getNewPath(), diffEntry);
                }
            } catch (GitAPIException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return Collections.unmodifiableMap(diffMap);
    }

    private CanonicalTreeParser getTree(String commitId) throws IOException {
        try (ObjectReader reader = repository.newObjectReader()) {
            var treeParser = new CanonicalTreeParser();
            var tree = repository.resolve(commitId);
            if (tree == null) {
                return null;
            } else {
                treeParser.reset(reader, tree);
                return treeParser;
            }
        }
    }
}
