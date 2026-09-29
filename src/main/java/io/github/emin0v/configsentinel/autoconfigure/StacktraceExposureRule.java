package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.springframework.core.env.Environment;

final class StacktraceExposureRule implements BuiltInConfigurationRule {

    private static final String ID = "server.stacktrace";
    private static final String PROPERTY = "server.error.include-stacktrace";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<ConfigurationViolation> evaluate(Environment environment) {
        String value = environment.getProperty(PROPERTY);
        if (value == null) {
            return Optional.empty();
        }

        String configuredValue = value.trim();
        if (!"always".equalsIgnoreCase(configuredValue)) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationViolation(ID, PROPERTY + " is set to '" + configuredValue + "'"));
    }
}
