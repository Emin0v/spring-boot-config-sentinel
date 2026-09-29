package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

final class ConfigurationEnforcer implements SmartInitializingSingleton {

    private static final Log LOGGER = LogFactory.getLog(ConfigurationEnforcer.class);
    private final ConfigurationRuleEngine ruleEngine;
    private final Environment environment;
    private final ConfigSentinelProperties properties;

    ConfigurationEnforcer(
            ConfigurationRuleEngine ruleEngine, Environment environment, ConfigSentinelProperties properties) {
        this.ruleEngine = ruleEngine;
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        enforce(ruleEngine, environment, properties, LOGGER);
    }

    static void enforce(
            ConfigurationRuleEngine ruleEngine,
            Environment environment,
            ConfigSentinelProperties properties,
            Log logger) {
        if (!properties.isEnabled()) {
            return;
        }

        properties.validate();
        String[] targetProfiles = properties.getProfiles().toArray(String[]::new);
        if (!environment.acceptsProfiles(Profiles.of(targetProfiles))) {
            return;
        }

        List<ConfigurationViolation> violations = ruleEngine.evaluate(environment);
        if (violations.isEmpty()) {
            return;
        }

        String report = formatReport(violations);
        if (properties.getAction() == ConfigSentinelProperties.Action.WARN) {
            logger.warn(report);
            return;
        }
        throw new IllegalStateException(report);
    }

    private static String formatReport(List<ConfigurationViolation> violations) {
        StringBuilder report = new StringBuilder("Config Sentinel found unsafe configuration:");
        for (ConfigurationViolation violation : violations) {
            report.append(System.lineSeparator())
                    .append("- [")
                    .append(violation.ruleId())
                    .append("] ")
                    .append(violation.message());
        }
        return report.toString();
    }
}
