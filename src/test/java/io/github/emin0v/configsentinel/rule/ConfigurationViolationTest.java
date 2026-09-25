package io.github.emin0v.configsentinel.rule;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationViolationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidRuleIdentifiers(String ruleId) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ConfigurationViolation(ruleId, "Unsafe configuration"))
                .withMessage("ruleId must not be blank");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidMessages(String message) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ConfigurationViolation("test.rule", message))
                .withMessage("message must not be blank");
    }
}
