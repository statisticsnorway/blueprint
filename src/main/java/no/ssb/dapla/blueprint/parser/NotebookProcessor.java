package no.ssb.dapla.blueprint.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.ssb.dapla.blueprint.notebook.Notebook;

import java.io.IOException;
import java.nio.file.Path;

public class NotebookProcessor {

    ObjectMapper mapper;

    Notebook process(Path dir) throws IOException {
        JsonNode jsonNode = mapper.readTree(dir.toFile());
        return new Notebook();
    }
}
