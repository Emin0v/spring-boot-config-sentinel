package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;

final class CustomPropertyRule implements ConfigurationRule {

    private static final String ID_PREFIX = "custom.";

    private final String id;
    private final String property;
    private final List<String> forbiddenValues;

    private CustomPropertyRule(String property, List<String> forbiddenValues) {
        this.id = ID_PREFIX + property;
        this.property = property;
        this.forbiddenValues = List.copyOf(forbiddenValues);
    }

    static List<ConfigurationRule> createRules(List<ConfigSentinelProperties.CustomRule> definitions) {
        if (definitions == null) {
            throw new IllegalArgumentException("config-sentinel.custom-rules must not be null");
        }

        List<ConfigurationRule> rules = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            ConfigSentinelProperties.CustomRule definition = definitions.get(index);
            validate(definition, index);
            rules.add(new CustomPropertyRule(definition.getProperty(), definition.getForbiddenValues()));
        }
        return List.copyOf(rules);
    }

    private static void validate(ConfigSentinelProperties.CustomRule definition, int index) {
        String path = "config-sentinel.custom-rules[" + index + "]";
        if (definition == null) {
            throw new IllegalArgumentException(path + " must not be null");
        }
        if (definition.getProperty() == null || definition.getProperty().isBlank()) {
            throw new IllegalArgumentException(path + ".property must not be blank");
        }

        List<String> forbiddenValues = definition.getForbiddenValues();
        if (forbiddenValues == null || forbiddenValues.isEmpty()) {
            throw new IllegalArgumentException(
                    path + ".forbidden-values must contain at least one value");
        }
        if (forbiddenValues.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(
                    path + ".forbidden-values must not contain blank values");
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Optional<ConfigurationViolation> evaluate(Environment environment) {
        String value = environment.getProperty(property);
        if (value == null || !forbiddenValues.contains(value)) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationViolation(
                id, "Property '" + property + "' matches a forbidden value"));
    }
}
