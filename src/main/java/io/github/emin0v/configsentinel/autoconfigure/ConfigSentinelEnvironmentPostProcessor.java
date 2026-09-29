package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import java.util.List;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public final class ConfigSentinelEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private final ConfigurationRuleEngine ruleEngine = new ConfigurationRuleEngine(builtInRules());
    private final Log logger;

    public ConfigSentinelEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(getClass());
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ConfigSentinelProperties properties = new ConfigSentinelProperties();
        Binder.get(environment).bind("config-sentinel", Bindable.ofInstance(properties));

        if (properties.getAction() == ConfigSentinelProperties.Action.FAIL) {
            ConfigurationEnforcer.enforce(ruleEngine, environment, properties, logger);
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
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
