package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.notebook.Notebook;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class NotebookProcessor {

    final ObjectMapper mapper;

    public NotebookProcessor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Notebook process(String path, String notebookPath) throws IOException {
        return process(Path.of(path), Path.of(notebookPath));
    }

    public Notebook process(Path path, Path notebookPath) throws IOException {

        JsonNode jsonNode = mapper.readTree(path.resolve(notebookPath).toFile());

        Notebook notebook = new Notebook();
        notebook.setPath(notebookPath);

        JsonNode cells = jsonNode.get("cells");
        processCells(notebook, cells);

        return notebook;

    }

    private void processCells(Notebook notebook, JsonNode cells) throws IOException {
        if (!cells.isArray()) {
            throw new IOException("cells was not an array");
        }
        for (JsonNode cell : cells) {
            JsonNode sources = cell.get("source");
            processSources(notebook, sources);
        }
    }

    private void processSources(Notebook notebook, JsonNode source) throws IOException {
        if (!source.isArray()) {
            throw new IOException("source was not an array");
        }

        Set<String> set = null;
        for (JsonNode line : source) {
            String textLine = line.asText().trim();
            // Ignore regular code.
            if (!textLine.startsWith("#")) {
                if (set != null) {
                    set = null;
                }
                continue;
            } else {
                textLine = textLine.substring(1);
            }

            if ("!inputs".equals(textLine)) {
                set = notebook.getInputs();
                continue;
            }
            if ("!outputs".equals(textLine)) {
                set = notebook.getOutputs();
                continue;
            }

            if (set != null) {
                set.add(textLine.trim());
            }
        }
    }
}
