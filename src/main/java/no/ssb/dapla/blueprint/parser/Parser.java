package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Commit;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import no.ssb.dapla.blueprint.neo4j.model.Repository;
import org.neo4j.driver.AuthTokens;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static picocli.CommandLine.Parameters;

public final class Parser {

    private final NotebookFileVisitor visitor;
    private final NotebookProcessor processor;
    private final Output output;

    public Parser(NotebookFileVisitor visitor, Output output) {
        this(visitor, output, new NotebookProcessor(new ObjectMapper()));
    }

    public Parser(NotebookFileVisitor visitor, Output output, NotebookProcessor processor) {
        this.visitor = Objects.requireNonNull(visitor);
        this.output = Objects.requireNonNull(output);
        this.processor = Objects.requireNonNull(processor);
    }

    public static void main(String... args) throws IOException {
        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.helpRequested) {
            CommandLine.usage(new Parser.Options(), System.err);
            return;
        }

        var auth = AuthTokens.none();
        if (options.user != null && options.password != null) {
            auth = AuthTokens.basic(options.user, options.password);
        }

        //var driver = GraphDatabase.driver(options.host, auth);


        //var output = new Neo4jOutput(new NotebookStore(driver));
        var output = new DebugOutput();
        var visitor = new NotebookFileVisitor(new HashSet<>(options.ignores));

        Parser parser = new Parser(visitor, output);

        // TODO: Refactor.
        var commit = new Commit(options.commitId);
        var repository = new Repository(options.repositoryURL);
        parser.parse(options.root.toPath(), commit, repository);

    }

    public void parse(Path path, Commit commit, Repository repository) throws IOException {
        // TODO: Refactor
        NotebookStore notebookStore = null;
        if (output instanceof Neo4jOutput) {
            notebookStore = ((Neo4jOutput) output).getNotebookStore();
        }

        GitNotebookProcessor notebookProcessor = null;
        if (processor instanceof GitNotebookProcessor) {
            notebookProcessor = (GitNotebookProcessor) processor;
        }

        // We get the commit and repository since we want to add a relation.
        var persistedRepo = notebookStore.findOrCreateRepository(repository.getId(), repository.getUri());
        var persistedCommit = notebookStore.findOrCreateCommit(commit.getId());

        Files.walkFileTree(path, visitor);
        for (Path notebookPath : visitor.getNotebooks()) {

            // (repo/foo/bar).relativize(repo/) -> foo/bar.
            var relNotebookPath = path.relativize(notebookPath);

            Notebook nb = processor.process(path, relNotebookPath);
            output.output(nb);

            var diff = notebookProcessor.get(relNotebookPath.toString());
            if (diff == null) {
                persistedCommit.addUnchanged(relNotebookPath, nb);
            } else {
                switch (diff.getChangeType()) {
                    case ADD -> {
                        persistedCommit.addCreate(relNotebookPath, nb);
                    }
                    // TODO: Add missing cases.
                    case MODIFY, RENAME, COPY -> {
                        persistedCommit.addUpdate(relNotebookPath, nb);
                    }
                    case DELETE -> {
                        persistedCommit.addDelete(relNotebookPath, nb);
                    }
                }
            }
        }

        persistedRepo.addCommit(persistedCommit);
        notebookStore.saveRepository(persistedRepo);

    }

    public final static class Options {

        @Option(required = true, names = {"-u", "--url"}, description = "Repository URL")
        public String repositoryURL;

        @Option(required = true, names = {"-c", "--commit"}, description = "Specify the commit to associate with the graph")
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
