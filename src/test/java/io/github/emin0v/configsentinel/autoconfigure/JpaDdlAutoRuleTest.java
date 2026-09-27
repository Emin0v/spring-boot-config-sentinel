package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class JpaDdlAutoRuleTest {

    private final JpaDdlAutoRule rule = new JpaDdlAutoRule();

    @ParameterizedTest
    @ValueSource(strings = {"update", "create", "create-drop", " UPDATE "})
    void flagsUnsafeModes(String value) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", value);

        Optional<ConfigurationViolation> result = rule.evaluate(environment);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().ruleId()).isEqualTo("jpa.ddl-auto");
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "validate"})
    void allowsSafeModes(String value) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", value);

        assertThat(rule.evaluate(environment)).isEmpty();
    }

    @Test
    void allowsAbsentValue() {
        assertThat(rule.evaluate(new MockEnvironment())).isEmpty();
    }
}
