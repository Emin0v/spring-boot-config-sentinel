package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class StacktraceExposureRuleTest {

    private final StacktraceExposureRule rule = new StacktraceExposureRule();

    @ParameterizedTest
    @ValueSource(strings = {"always", " ALWAYS "})
    void flagsAlways(String value) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.error.include-stacktrace", value);

        assertThat(rule.evaluate(environment))
                .hasValueSatisfying(violation -> assertThat(violation.ruleId())
                        .isEqualTo("server.stacktrace"));
    }

    @Test
    void allowsNever() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("server.error.include-stacktrace", "never");

        assertThat(rule.evaluate(environment)).isEmpty();
    }

    @Test
    void allowsAbsentValue() {
        assertThat(rule.evaluate(new MockEnvironment())).isEmpty();
    }
}
