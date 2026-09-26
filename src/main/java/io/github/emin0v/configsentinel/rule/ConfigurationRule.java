package io.github.emin0v.configsentinel.rule;

import java.util.Optional;
import org.springframework.core.env.Environment;

public interface ConfigurationRule {

    String id();

    Optional<ConfigurationViolation> evaluate(Environment environment);
}
