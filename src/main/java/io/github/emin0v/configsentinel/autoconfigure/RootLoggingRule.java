package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.env.Environment;

final class RootLoggingRule implements BuiltInConfigurationRule {

    private static final String ID = "logging.root-level";
    private static final String PROPERTY = "logging.level.root";
    private static final Set<String> UNSAFE_VALUES = Set.of("DEBUG", "TRACE");

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
        boolean unsafe = UNSAFE_VALUES.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(configuredValue));
        if (!unsafe) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationViolation(ID, PROPERTY + " is set to '" + configuredValue + "'"));
    }
}
