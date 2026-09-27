package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class RootLoggingRuleTest {

    private final RootLoggingRule rule = new RootLoggingRule();

    @ParameterizedTest
    @ValueSource(strings = {"DEBUG", "TRACE", "debug", " trace "})
    void flagsVerboseLevels(String value) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("logging.level.root", value);

        assertThat(rule.evaluate(environment))
                .hasValueSatisfying(violation -> assertThat(violation.ruleId())
                        .isEqualTo("logging.root-level"));
    }

    @Test
    void allowsInfo() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("logging.level.root", "INFO");

        assertThat(rule.evaluate(environment)).isEmpty();
    }

    @Test
    void allowsAbsentValue() {
        assertThat(rule.evaluate(new MockEnvironment())).isEmpty();
    }
}
