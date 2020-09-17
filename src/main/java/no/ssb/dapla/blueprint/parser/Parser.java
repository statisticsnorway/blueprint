package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.neo4j.NotebookStore;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.neo4j.driver.AuthTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

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

        parser.parse(options.root.toPath(), options.commitId, URI.create(options.repositoryURL));

    }

    public void parse(Path repositoryPath, String commitId, URI repositoryURI) throws IOException {

        log.info("parsing commit {} from repository {} (checked out in {})", commitId, repositoryURI, repositoryPath);
        try {

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
            var persistedRepo = notebookStore.findOrCreateRepository(repositoryURI);
            var persistedCommit = notebookStore.findOrCreateCommit(commitId);

            Files.walkFileTree(repositoryPath, visitor);
            for (Path absolutePath : visitor.getNotebooks()) {

                // (repo/foo/bar).relativize(repo/) -> foo/bar.
                var relativePath = repositoryPath.relativize(absolutePath);

                Notebook nb = processor.process(repositoryPath, relativePath);
                output.output(nb);

                var diff = notebookProcessor.get(relativePath.toString());
                if (diff == null) {
                    persistedCommit.addUnchanged(relativePath, nb);
                } else {
                    switch (diff.getChangeType()) {
                        case ADD -> {
                            persistedCommit.addCreate(relativePath, nb);
                        }
                        // TODO: Add missing cases.
                        case MODIFY, RENAME, COPY -> {
                            persistedCommit.addUpdate(relativePath, nb);
                        }
                        case DELETE -> {
                            persistedCommit.addDelete(relativePath, nb);
                        }
                    }
                }
            }

            persistedRepo.addCommit(persistedCommit);
            notebookStore.saveRepository(persistedRepo);

        } catch (Exception ex) {
            log.warn("failed to parse commit {} from repository {} (checked out in {})", commitId, repositoryURI,
                    repositoryPath, ex);
        }

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
