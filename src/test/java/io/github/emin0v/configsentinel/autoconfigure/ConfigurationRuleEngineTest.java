package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class ConfigurationRuleEngineTest {

    private final Environment environment = new MockEnvironment();

    @Test
    void returnsNoViolationsWithoutRules() {
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of());

        assertThat(engine.evaluate(environment)).isEmpty();
    }

    @Test
    void excludesPassingRules() {
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of(passingRule("passing.rule")));

        assertThat(engine.evaluate(environment)).isEmpty();
    }

    @Test
    void returnsViolationsInRuleIdOrder() {
        ConfigurationViolation firstViolation = new ConfigurationViolation("alpha.rule", "First violation");
        ConfigurationViolation secondViolation = new ConfigurationViolation("bravo.rule", "Second violation");
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of(
                failingRule(secondViolation),
                passingRule("charlie.rule"),
                failingRule(firstViolation)));

        assertThat(engine.evaluate(environment)).containsExactly(firstViolation, secondViolation);
    }

    @Test
    void evaluatesAllRules() {
        List<String> evaluatedRuleIds = new ArrayList<>();
        ConfigurationRule firstRule = rule("first.rule", ignored -> {
            evaluatedRuleIds.add("first.rule");
            return Optional.empty();
        });
        ConfigurationRule secondRule = rule("second.rule", ignored -> {
            evaluatedRuleIds.add("second.rule");
            return Optional.empty();
        });
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of(secondRule, firstRule));

        engine.evaluate(environment);

        assertThat(evaluatedRuleIds).containsExactly("first.rule", "second.rule");
    }

    @Test
    void rejectsDuplicateRuleIds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ConfigurationRuleEngine(List.of(
                        passingRule("duplicate.rule"),
                        passingRule("duplicate.rule"))))
                .withMessage("Duplicate rule identifier: duplicate.rule");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankRuleIds(String ruleId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ConfigurationRuleEngine(List.of(passingRule(ruleId))))
                .withMessage("Rule identifier must not be blank");
    }

    @Test
    void rejectsNullRuleResults() {
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of(rule("broken.rule", ignored -> null)));

        assertThatNullPointerException()
                .isThrownBy(() -> engine.evaluate(environment))
                .withMessage("Rule 'broken.rule' returned null");
    }

    @Test
    void rejectsViolationsAttributedToAnotherRule() {
        ConfigurationRule rule = rule("actual.rule", ignored -> Optional.of(
                new ConfigurationViolation("different.rule", "Unsafe configuration")));
        ConfigurationRuleEngine engine = new ConfigurationRuleEngine(List.of(rule));

        assertThatIllegalStateException()
                .isThrownBy(() -> engine.evaluate(environment))
                .withMessage("Rule 'actual.rule' produced a violation for rule 'different.rule'");
    }

    private static ConfigurationRule passingRule(String ruleId) {
        return rule(ruleId, ignored -> Optional.empty());
    }

    private static ConfigurationRule failingRule(ConfigurationViolation violation) {
        return rule(violation.ruleId(), ignored -> Optional.of(violation));
    }

    private static ConfigurationRule rule(
            String ruleId, Function<Environment, Optional<ConfigurationViolation>> evaluator) {
        return new ConfigurationRule() {
            @Override
            public String id() {
                return ruleId;
            }

            @Override
            public Optional<ConfigurationViolation> evaluate(Environment environment) {
                return evaluator.apply(environment);
            }
        };
    }
}
