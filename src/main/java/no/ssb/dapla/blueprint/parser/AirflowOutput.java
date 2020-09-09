package no.ssb.dapla.blueprint.parser;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import no.ssb.dapla.blueprint.neo4j.model.Dependency;
import no.ssb.dapla.blueprint.neo4j.model.Notebook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Output that generates airflow file.
 */
public class AirflowOutput implements Output {

    private static final Logger log = LoggerFactory.getLogger(AirflowOutput.class);

    private final Map<String, Set<Notebook>> consumers = new HashMap<>();
    private final Map<String, Set<Notebook>> producers = new HashMap<>();
    private final Set<Notebook> notebooks = new HashSet<>();
    private final Configuration templateConfig = new Configuration(Configuration.VERSION_2_3_29);
    private final Template template;

    public AirflowOutput() throws IOException {
        // TODO: Documentation recommends saving this as a singleton.
        templateConfig.setTemplateLoader(new ClassTemplateLoader(AirflowOutput.class, "/"));
        templateConfig.setDefaultEncoding("UTF-8");
        templateConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templateConfig.setLogTemplateExceptions(false);
        templateConfig.setWrapUncheckedExceptions(true);
        templateConfig.setFallbackOnNullLoopVariable(false);
        template = templateConfig.getTemplate("airflow-dag.template.py");
    }

    @Override
    public void output(Notebook notebook) {
        for (String input : notebook.getInputs()) {
            consumers.computeIfAbsent(input, s -> new HashSet<>()).add(notebook);
        }
        for (String output : notebook.getOutputs()) {
            producers.computeIfAbsent(output, s -> new HashSet<>()).add(notebook);
        }
        notebooks.add(notebook);
    }

    // TODO:
    public void close() throws TemplateException, IOException {

        // Transform the data so it is easier to work with. Should probably be
        // revisited at some point for performances. I am using a set to avoid
        // duplicates. Therefore the equals and hashCode implementation of
        // Notebook and Dependency are important.
        Set<Dependency> dependencies = new HashSet<>();
        for (var notebook : notebooks) {
            for (var output : notebook.getOutputs()) {
                for (var consumer : consumers.getOrDefault(output, Set.of())) {
                    dependencies.add(new Dependency(notebook, consumer));
                }
            }
        }

        var out = new OutputStreamWriter(System.out);
        var model = Map.of(
                "consumers", consumers,
                "notebooks", notebooks,
                "producers", producers,
                "dependencies", dependencies
        );

        template.process(model, out);
        out.flush();
    }

}
