package no.ssb.dapla.blueprint.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class NotebookFileVisitor extends SimpleFileVisitor<Path> {

    static final Logger log = LoggerFactory.getLogger(NotebookFileVisitor.class);

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        log.debug("visiting file {}", file);
        return FileVisitResult.CONTINUE;
    }
}
