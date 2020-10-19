package no.ssb.dapla.blueprint.parser;

import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static no.ssb.dapla.blueprint.BlueprintApplication.initNeo4jDriver;
import static picocli.CommandLine.Parameters;

public final class Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

    private final NotebookFileVisitor visitor;
    private final NotebookProcessor processor;
    private final GitHelper gitHelper;
    private final NotebookStore notebookStore;

    public Parser(Repository repository, NotebookStore store, Set<String> ignores) {
        this.gitHelper = new GitHelper(Objects.requireNonNull(repository));
        this.visitor = new NotebookFileVisitor(ignores);
        this.processor = new NotebookProcessor();
        this.notebookStore = Objects.requireNonNull(store);
    }

    public Parser(Repository repository, NotebookStore store) {
        this(repository, store, Set.of(".git"));
    }

    public static void main(String... args) throws IOException, GitAPIException {
        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.helpRequested) {
            CommandLine.usage(new Parser.Options(), System.err);
            return;
        }

        var sessionFactory = initNeo4jDriver(options.host.toString(), options.user, options.password, 10);
        var repository = new FileRepositoryBuilder()
                .setWorkTree(options.root).setup().build();
        HashSet<String> ignores = new HashSet<>(options.ignores);
        ignores.add(".git");

        Parser parser = new Parser(repository, new NotebookStore(sessionFactory), ignores);
        if (options.commitId.contains(":")) {
            String[] range = options.commitId.split(":");
            parser.parse(options.root.toPath(), URI.create(options.repositoryURL), range[0], range[1]);
        } else {
            parser.parse(options.root.toPath(), options.commitId, URI.create(options.repositoryURL));
        }
        sessionFactory.close();
    }

    public void parse(Path repositoryPath, URI repositoryURI, String revFrom, String revTo) throws IOException, GitAPIException {
        for (String commitId : gitHelper.getRange(revFrom, revTo)) {
            parse(repositoryPath, commitId, repositoryURI);
        }
    }

    public void parse(Path repositoryPath, String commitId, URI repositoryURI) {

        log.info("parsing commit {} from repository {} (checked out in {})", commitId, repositoryURI, repositoryPath);
        try {

            gitHelper.checkout(commitId);
            Map<String, DiffEntry> diffMap = gitHelper.getDiffMap(commitId);

            // We get the commit and repository since we want to add a relation.
            var persistedRepo = notebookStore.findOrCreateRepository(repositoryURI);

            var persistedCommit = notebookStore.findOrCreateCommit(commitId);

            RevCommit commit = gitHelper.getCommit(commitId);

            persistedCommit.setAuthorName(commit.getAuthorIdent().getName());
            persistedCommit.setAuthorEmail(commit.getAuthorIdent().getEmailAddress());
            persistedCommit.setAuthoredAt(commit.getAuthorIdent().getWhen().toInstant()
                    .atZone(commit.getAuthorIdent().getTimeZone().toZoneId()).toInstant());

            persistedCommit.setCommitterName(commit.getAuthorIdent().getName());
            persistedCommit.setCommitterEmail(commit.getAuthorIdent().getEmailAddress());
            persistedCommit.setCommittedAt(commit.getAuthorIdent().getWhen().toInstant()
                    .atZone(commit.getAuthorIdent().getTimeZone().toZoneId()).toInstant());

            persistedCommit.setMessage(commit.getFullMessage());

            Files.walkFileTree(repositoryPath, visitor);
            for (Path absolutePath : visitor.getNotebooks()) {

                // (repo/foo/bar).relativize(repo/) -> foo/bar.
                var relativePath = repositoryPath.relativize(absolutePath);

                Notebook nb = processor.process(repositoryPath, relativePath);

                nb.setBlobId(gitHelper.getObjectId(commitId, relativePath));

                if (diffMap.containsKey(relativePath.toString())) {
                    switch (diffMap.get(relativePath.toString()).getChangeType()) {
                        case ADD -> persistedCommit.addCreate(relativePath, nb);
                        // TODO: Add missing cases.
                        case MODIFY, RENAME, COPY -> persistedCommit.addUpdate(relativePath, nb);
                        case DELETE -> {
                            // it will never reach this, as deleted notebooks are not present in the notebooks list
                        }
                    }
                } else {
                    persistedCommit.addUnchanged(relativePath, nb);
                }
            }

            // Add deleted files
            diffMap.values().stream()
                    .filter(entry -> entry.getChangeType().equals(DiffEntry.ChangeType.DELETE))
                    .forEach(entry -> persistedCommit
                            .addDelete(entry.getOldPath(), new Notebook(entry.getId(DiffEntry.Side.OLD).toObjectId().getName())));

            persistedRepo.addCommit(persistedCommit);
            notebookStore.saveRepository(persistedRepo);

        } catch (Exception ex) {
            log.warn("failed to parse commit {} from repository {} (checked out in {})", commitId, repositoryURI,
                    repositoryPath, ex);
        } finally {
            visitor.close();
        }

    }

    public final static class Options {

        @Option(required = true, names = {"-u", "--url"}, description = "Repository URL")
        public String repositoryURL;

        @Option(required = true, names = {"-c", "--commit"}, description = "Specify the commit or range of commit to process")
        public String commitId;

        @Option(names = {"-i", "--ignore"}, description = "folders to ignore")
        public List<String> ignores = List.of();

        @Option(required = true, names = "--host", description = "Neo4J host")
        public URI host;

        @Option(names = "--user", description = "Neo4J username")
        public String user;

        @Option(names = "--password", description = "Neo4J password")
        public String password;

        @Parameters(paramLabel = "ROOT", description = "the root file where to search for notebooks")
        public File root;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
        public boolean helpRequested = false;
    }
}
