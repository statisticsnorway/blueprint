package no.ssb.dapla.blueprint;

import io.helidon.config.Config;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.helidon.config.ConfigSources.classpath;

public class TestConfigExtension implements BeforeAllCallback {

    private Config config;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        createConfig();
    }

    public Config getConfig() {
        createConfig();
        return config;
    }

    private void createConfig() {
        if (this.config == null) {
            this.config = Config
                    .builder(classpath("application-dev.yaml"),
                            classpath("application.yaml"))
                    .metaConfig()
                    .build();
        }
    }
}
