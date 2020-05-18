package no.ssb.dapla.blueprint.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NotebookFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger log = LoggerFactory.getLogger(NotebookFileVisitor.class);
    private final Parser.Options options;
    private final Pattern fileExtension = Pattern.compile("\\*\\.ipynb");
    private final List<Path> notebooks = new ArrayList<>();

    public NotebookFileVisitor(Parser.Options options) {
        this.options = options;
    }

    public List<Path> getNotebooks() {
        return notebooks;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (options.ignores.contains(dir.getFileName().toString())) {
            log.warn("ignoring {}", dir);
            return FileVisitResult.SKIP_SUBTREE;
        } else {
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.debug("visiting file {}", file);
        if (fileExtension.matcher(file.getFileName().toString()).matches()) {
            notebooks.add(file);
        }
        return FileVisitResult.CONTINUE;
    }
}
