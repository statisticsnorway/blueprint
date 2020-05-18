package no.ssb.dapla.blueprint.parser;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static picocli.CommandLine.Parameters;

public final class Parser {

    public static void main(String... args) throws IOException {
        Options options = CommandLine.populateCommand(new Options(), args);
        if (options.helpRequested) {
            return;
        }

        NotebookFileVisitor fileVisitor = new NotebookFileVisitor();
        Files.walkFileTree(options.root.toPath(), fileVisitor);



    }

    public final static class Options {

        @Option(names = {"-c", "--commit"}, description = "Specify the commit to associate with the graph")
        String commitId;

        @Option(names = {"-o", "--output"}, description = "Output format")
        String output;

        @Parameters(paramLabel = "ROOT", description = "the root file where to search for notebooks")
        File root;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
        boolean helpRequested = false;
    }
}
