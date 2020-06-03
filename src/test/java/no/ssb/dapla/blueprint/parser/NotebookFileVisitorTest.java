package no.ssb.dapla.blueprint.parser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotebookFileVisitorTest {

    private Path tempPath;

    @BeforeEach
    void setUp() throws IOException {
        tempPath = Files.createTempDirectory("dapla-blueprint-visitor-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempPath).sorted(Comparator.reverseOrder()).forEach(t -> {
            try {
                Files.delete(t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testIgnoringFolders() throws IOException {

        List<Path> paths = List.of(
                tempPath.resolve("should").resolve("visitThis").resolve("file.ipynb"),
                tempPath.resolve("should").resolve("ignoreThis").resolve("file.ipynb"),
                tempPath.resolve("should").resolve(".ignoreThat").resolve("file.ipynb"),
                tempPath.resolve("should").resolve("visitThat").resolve("file.ipynb")
        );

        for (Path path : paths) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        var ignores = List.of("ignoreThis", ".ignoreThat");
        NotebookFileVisitor notebookFileVisitor = new NotebookFileVisitor(ignores);
        Files.walkFileTree(tempPath, notebookFileVisitor);

        assertThat(notebookFileVisitor.getNotebooks()).containsExactlyInAnyOrder(
                tempPath.resolve("should").resolve("visitThis").resolve("file.ipynb"),
                tempPath.resolve("should").resolve("visitThat").resolve("file.ipynb")
        );
    }

    @Test
    void testOnlyIncludesIpynb() throws IOException {
        List<Path> paths = List.of(
                tempPath.resolve("should").resolve("visitThis").resolve("file.ipynb"),
                tempPath.resolve("should").resolve("notVisit").resolve("file.txt"),
                tempPath.resolve("should").resolve("visitThat").resolve("file.ipynb")
        );

        for (Path path : paths) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        NotebookFileVisitor notebookFileVisitor = new NotebookFileVisitor(Collections.emptyList());
        Files.walkFileTree(tempPath, notebookFileVisitor);

        assertThat(notebookFileVisitor.getNotebooks()).containsExactlyInAnyOrder(
                tempPath.resolve("should").resolve("visitThis").resolve("file.ipynb"),
                tempPath.resolve("should").resolve("visitThat").resolve("file.ipynb")
        );
    }
}