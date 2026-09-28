package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class ActuatorExposureRuleTest {

    private final ActuatorExposureRule rule = new ActuatorExposureRule();

    @ParameterizedTest
    @ValueSource(strings = {"*", "health,info,*"})
    void flagsExposeAll(String value) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("management.endpoints.web.exposure.include", value);

        assertThat(rule.evaluate(environment))
                .hasValueSatisfying(violation -> {
                    assertThat(violation.ruleId()).isEqualTo("actuator.web-exposure");
                    assertThat(violation.message())
                            .isEqualTo("management.endpoints.web.exposure.include exposes all endpoints");
                });
    }

    @Test
    void flagsIndexedExposeAll() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("management.endpoints.web.exposure.include[0]", "health")
                .withProperty("management.endpoints.web.exposure.include[1]", "*");

        assertThat(rule.evaluate(environment)).isPresent();
    }

    @Test
    void allowsSelectedEndpoints() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("management.endpoints.web.exposure.include", "health,info");

        assertThat(rule.evaluate(environment)).isEmpty();
    }

    @Test
    void allowsAbsentValue() {
        assertThat(rule.evaluate(new MockEnvironment())).isEmpty();
    }
}
