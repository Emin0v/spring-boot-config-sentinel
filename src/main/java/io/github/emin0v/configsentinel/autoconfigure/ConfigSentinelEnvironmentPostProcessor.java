package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

public final class ConfigSentinelEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROCESSED_PROPERTY =
            ConfigSentinelEnvironmentPostProcessor.class.getName() + ".built-in-rules-processed";
    private static final String PROPERTY_SOURCE_NAME =
            ConfigSentinelEnvironmentPostProcessor.class.getName();

    private final ConfigurationRuleEngine ruleEngine = new ConfigurationRuleEngine(builtInRules());
    private final Log logger;

    public ConfigSentinelEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ConfigSentinelProperties properties = new ConfigSentinelProperties();
        Binder.get(environment).bind("config-sentinel", Bindable.ofInstance(properties));

        ConfigurationEnforcer.enforce(ruleEngine, environment, properties, logger);
        environment.getPropertySources().addFirst(new MapPropertySource(
                PROPERTY_SOURCE_NAME, Map.of(PROCESSED_PROPERTY, true)));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    static boolean hasProcessedBuiltInRules(Environment environment) {
        return environment.getProperty(PROCESSED_PROPERTY, Boolean.class, false);
    }

    private static List<ConfigurationRule> builtInRules() {
        return List.of(
                new JpaDdlAutoRule(),
                new JpaShowSqlRule(),
                new ActuatorExposureRule(),
                new StacktraceExposureRule(),
                new RootLoggingRule());
    }
}
