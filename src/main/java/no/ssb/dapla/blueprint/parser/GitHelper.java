package no.ssb.dapla.blueprint.parser;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public void checkout(String commitId) throws GitAPIException {
        try (Git git = Git.wrap(repository)) {
            git.checkout().setName(commitId).call();
        }
    }

    public RevCommit getCommit(String rev) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId objectId = repository.resolve(rev);
            RevCommit commit = walk.parseCommit(objectId);
            walk.dispose();
            return commit;
        }
    }

    public List<String> getRange(String revFrom, String revTo) throws IOException, GitAPIException {
        ObjectId from = repository.resolve(revFrom);
        ObjectId to = repository.resolve(revTo);
        try (Git git = Git.wrap(repository)) {
            return StreamSupport.stream(git.log().addRange(from, to).call().spliterator(), false)
                    .map(revCommit -> revCommit.getId().getName())
                    .collect(Collectors.toList());
        }
    }

    public String getHead() throws IOException {
        ObjectId id = repository.resolve(Constants.HEAD);
        return id.getName();
    }

    public Map<String, DiffEntry> getDiffMap(String commitId) throws IOException {
        Map<String, DiffEntry> diffMap = new HashMap<>();
        AbstractTreeIterator currentTree = getTree(commitId + "^{tree}");
        AbstractTreeIterator previousTree = getTree(commitId + "~1^{tree}");
        if (previousTree == null) {
            // Sha of empty tree.
            previousTree = new EmptyTreeIterator();
        }
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
