package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

class CustomPropertyRuleTest {

    private static final String PROPERTY = "payment.mock-enabled";

    @Test
    void flagsExactForbiddenValueWithoutDisclosingIt() {
        ConfigurationRule rule = rule(PROPERTY, "sensitive-value");
        MockEnvironment environment = new MockEnvironment()
                .withProperty(PROPERTY, "sensitive-value");

        assertThat(rule.evaluate(environment))
                .hasValueSatisfying(violation -> {
                    assertThat(violation.ruleId()).isEqualTo("custom.payment.mock-enabled");
                    assertThat(violation.message())
                            .isEqualTo("Property 'payment.mock-enabled' matches a forbidden value")
                            .doesNotContain("sensitive-value");
                });
    }

    @Test
    void comparesValuesExactlyAndCaseSensitively() {
        ConfigurationRule rule = rule(PROPERTY, "true");

        assertThat(rule.evaluate(new MockEnvironment().withProperty(PROPERTY, "TRUE"))).isEmpty();
        assertThat(rule.evaluate(new MockEnvironment().withProperty(PROPERTY, " true "))).isEmpty();
        assertThat(rule.evaluate(new MockEnvironment().withProperty(PROPERTY, "true"))).isPresent();
    }

    @Test
    void allowsMissingProperty() {
        assertThat(rule(PROPERTY, "true").evaluate(new MockEnvironment())).isEmpty();
    }

    @Test
    void supportsMultipleForbiddenValues() {
        ConfigurationRule rule = rule(PROPERTY, "true", "mock", "disabled");

        assertThat(rule.evaluate(new MockEnvironment().withProperty(PROPERTY, "mock"))).isPresent();
        assertThat(rule.evaluate(new MockEnvironment().withProperty(PROPERTY, "false"))).isEmpty();
    }

    @Test
    void resolvesEnvironmentVariableValuesAsStrings() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                "testEnvironment", java.util.Map.of("PAYMENT_MOCK_ENABLED", "true")));

        assertThat(rule(PROPERTY, "true").evaluate(environment)).isPresent();
    }

    @Test
    void rejectsMissingPropertyName() {
        ConfigSentinelProperties.CustomRule definition = definition(null, List.of("true"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CustomPropertyRule.createRules(List.of(definition)))
                .withMessage("config-sentinel.custom-rules[0].property must not be blank");
    }

    @Test
    void rejectsEmptyForbiddenValues() {
        ConfigSentinelProperties.CustomRule definition = definition(PROPERTY, List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CustomPropertyRule.createRules(List.of(definition)))
                .withMessage("config-sentinel.custom-rules[0].forbidden-values must contain at least one value");
    }

    @Test
    void rejectsBlankAndNullForbiddenValuesWithoutDisclosingEntries() {
        List<String> forbiddenValues = Arrays.asList("sensitive-value", null, " ");
        ConfigSentinelProperties.CustomRule definition = definition(PROPERTY, forbiddenValues);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CustomPropertyRule.createRules(List.of(definition)))
                .withMessage("config-sentinel.custom-rules[0].forbidden-values must not contain blank values")
                .withMessageNotContaining("sensitive-value");
    }

    private static ConfigurationRule rule(String property, String... forbiddenValues) {
        return CustomPropertyRule.createRules(List.of(definition(property, List.of(forbiddenValues))))
                .getFirst();
    }

    private static ConfigSentinelProperties.CustomRule definition(
            String property, List<String> forbiddenValues) {
        ConfigSentinelProperties.CustomRule definition = new ConfigSentinelProperties.CustomRule();
        definition.setProperty(property);
        definition.setForbiddenValues(forbiddenValues);
        return definition;
    }
}
