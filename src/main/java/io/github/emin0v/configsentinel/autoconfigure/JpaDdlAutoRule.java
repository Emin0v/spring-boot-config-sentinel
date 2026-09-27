package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.env.Environment;

final class JpaDdlAutoRule implements ConfigurationRule {

    private static final String ID = "jpa.ddl-auto";
    private static final String PROPERTY = "spring.jpa.hibernate.ddl-auto";
    private static final Set<String> UNSAFE_VALUES = Set.of("update", "create", "create-drop");

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
