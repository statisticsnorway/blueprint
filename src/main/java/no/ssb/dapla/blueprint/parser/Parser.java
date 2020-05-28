package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.NotebookStore;
import no.ssb.dapla.blueprint.notebook.Notebook;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static picocli.CommandLine.Parameters;

public final class Parser {

    public static void main(String... args) throws IOException {
        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.helpRequested) {
            return;
        }

        var auth = AuthTokens.none();
        if (options.user != null && options.password != null) {
            auth = AuthTokens.basic(options.user, options.password);
        }

        var driver = GraphDatabase.driver(options.host, auth);
        var output = new Neo4jOutput(new NotebookStore(driver));
        var processor = new NotebookProcessor(new ObjectMapper());

        NotebookFileVisitor fileVisitor = new NotebookFileVisitor(options);
        Files.walkFileTree(options.root.toPath(), fileVisitor);

        for (Path notebook : fileVisitor.getNotebooks()) {
            Notebook nb = processor.process(notebook);
            nb.commitId = options.commitId;
            nb.repositoryURL = options.repositoryURL;
            output.output(nb);
        }

    }

    public static void parse(Options options) {

        var auth = AuthTokens.none();
        if (options.user != null && options.password != null) {
            auth = AuthTokens.basic(options.user, options.password);
        }

        var driver = GraphDatabase.driver(options.host, auth);
        var output = new Neo4jOutput(new NotebookStore(driver));
        var processor = new NotebookProcessor(new ObjectMapper());

        NotebookFileVisitor fileVisitor = new NotebookFileVisitor(options);
        try {
            Files.walkFileTree(options.root.toPath(), fileVisitor);
            for (Path notebook : fileVisitor.getNotebooks()) {
                Notebook nb = processor.process(notebook);
                nb.commitId = options.commitId;
                nb.repositoryURL = options.repositoryURL;
                output.output(nb);
            }
        } catch (IOException e) {
            e.printStackTrace(); // TODO handle
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
