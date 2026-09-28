package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JpaShowSqlRuleTest {

    private final JpaShowSqlRule rule = new JpaShowSqlRule();

    @Test
    void flagsEnabledSqlLogging() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.jpa.show-sql", "true");

        assertThat(rule.evaluate(environment))
                .hasValueSatisfying(violation -> {
                    assertThat(violation.ruleId()).isEqualTo("jpa.show-sql");
                    assertThat(violation.message()).isEqualTo("spring.jpa.show-sql is set to 'true'");
                });
    }

    @Test
    void allowsDisabledSqlLogging() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.jpa.show-sql", "false");

        assertThat(rule.evaluate(environment)).isEmpty();
    }

    @Test
    void allowsAbsentValue() {
        assertThat(rule.evaluate(new MockEnvironment())).isEmpty();
    }
}
