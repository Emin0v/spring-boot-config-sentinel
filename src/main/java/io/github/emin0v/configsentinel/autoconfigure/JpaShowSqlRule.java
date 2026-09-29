package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.springframework.core.env.Environment;

final class JpaShowSqlRule implements BuiltInConfigurationRule {

    private static final String ID = "jpa.show-sql";
    private static final String PROPERTY = "spring.jpa.show-sql";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<ConfigurationViolation> evaluate(Environment environment) {
        if (!Boolean.TRUE.equals(environment.getProperty(PROPERTY, Boolean.class))) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationViolation(ID, PROPERTY + " is set to 'true'"));
    }
}
