package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.notebook.Notebook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NotebookProcessor {

    final ObjectMapper mapper;

    public NotebookProcessor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Notebook process(File path) throws IOException {
        return process(path.toPath());
    }

    public Notebook process(Path path) throws IOException {

        JsonNode jsonNode = mapper.readTree(path.toFile());

        Notebook notebook = new Notebook();
        notebook.fileName = path.getFileName().toString();
        notebook.path = path.toString();

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

        List<String> list = null;
        for (JsonNode line : source) {
            String textLine = line.asText();
            if ("#!inputs".equals(textLine)) {
                list = notebook.inputs;
                continue;
            }
            if ("#!outputs".equals(textLine)) {
                list = notebook.outputs;
                continue;
            }
            if (list != null) {
                list.add(textLine.trim());
            }
        }
    }
}
