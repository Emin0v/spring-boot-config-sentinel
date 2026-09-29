package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.springframework.core.env.Environment;

final class ConfigurationRuleEngine {

    private final Map<String, ConfigurationRule> rulesById;

    ConfigurationRuleEngine(List<ConfigurationRule> rules) {
        Objects.requireNonNull(rules, "rules must not be null");

        Map<String, ConfigurationRule> registeredRules = new TreeMap<>();
        for (ConfigurationRule rule : rules) {
            Objects.requireNonNull(rule, "rules must not contain null");
            String ruleId = rule.id();
            if (ruleId == null || ruleId.isBlank()) {
                throw new IllegalArgumentException("Rule identifier must not be blank");
            }
            if (registeredRules.putIfAbsent(ruleId, rule) != null) {
                throw new IllegalArgumentException("Duplicate rule identifier: " + ruleId);
            }
        }
        this.rulesById = Collections.unmodifiableMap(registeredRules);
    }

    List<ConfigurationViolation> evaluate(Environment environment) {
        return evaluate(environment, rule -> true);
    }

    List<ConfigurationViolation> evaluate(
            Environment environment, Predicate<ConfigurationRule> ruleFilter) {
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(ruleFilter, "ruleFilter must not be null");

        return rulesById.entrySet().stream()
                .filter(entry -> ruleFilter.test(entry.getValue()))
                .map(entry -> evaluate(entry.getKey(), entry.getValue(), environment))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ConfigurationViolation> evaluate(
            String ruleId, ConfigurationRule rule, Environment environment) {
        Optional<ConfigurationViolation> result = Objects.requireNonNull(
                rule.evaluate(environment), "Rule '" + ruleId + "' returned null");
        result.ifPresent(violation -> {
            if (!ruleId.equals(violation.ruleId())) {
                throw new IllegalStateException(
                        "Rule '" + ruleId + "' produced a violation for rule '" + violation.ruleId() + "'");
            }
        });
        return result;
    }
}
