package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

final class ActuatorExposureRule implements ConfigurationRule {

    private static final String ID = "actuator.web-exposure";
    private static final String PROPERTY = "management.endpoints.web.exposure.include";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<ConfigurationViolation> evaluate(Environment environment) {
        List<String> endpoints = Binder.get(environment)
                .bind(PROPERTY, Bindable.listOf(String.class))
                .orElseGet(List::of);
        boolean exposesAll = endpoints.stream().map(String::trim).anyMatch("*"::equals);
        if (!exposesAll) {
            return Optional.empty();
        }
        return Optional.of(new ConfigurationViolation(ID, PROPERTY + " exposes all endpoints"));
    }
}
