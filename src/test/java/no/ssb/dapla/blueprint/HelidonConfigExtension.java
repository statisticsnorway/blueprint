package no.ssb.dapla.blueprint;

import io.helidon.config.Config;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import static io.helidon.config.ConfigSources.classpath;

public class HelidonConfigExtension extends TypeBasedParameterResolver<Config> implements BeforeAllCallback {

    private Config config;

    @Override
    public Config resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return config;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
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
