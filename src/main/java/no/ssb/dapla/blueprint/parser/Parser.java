package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
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

        NotebookFileVisitor fileVisitor = new NotebookFileVisitor(options);
        Files.walkFileTree(options.root.toPath(), fileVisitor);

        // Select output?
        var output = new DebugOutput();
        var processor = new NotebookProcessor(new ObjectMapper());

        for (Path notebook : fileVisitor.getNotebooks()) {
            output.output(processor.process(notebook));
        }
        
    }

    public final static class Options {

        @Option(names = {"-c", "--commit"}, description = "Specify the commit to associate with the graph")
        String commitId;

        @Option(names = {"-i", "--ignore"}, description = "folders to ignore")
        List<String> ignores = List.of();

        @Option(names = {"-o", "--output"}, description = "Output format")
        String output;

        @Parameters(paramLabel = "ROOT", description = "the root file where to search for notebooks")
        File root;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
        boolean helpRequested = false;
    }
}
