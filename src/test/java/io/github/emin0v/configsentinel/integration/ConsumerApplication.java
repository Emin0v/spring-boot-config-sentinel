package io.github.emin0v.configsentinel.integration;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
class ConsumerApplication {

    @Bean
    RuleDependency ruleDependency() {
        return new RuleDependency();
    }

    @Bean
    ConfigurationRule javaCustomRule(RuleDependency dependency) {
        return new JavaCustomRule(dependency);
    }

    static final class RuleDependency {

        boolean isReady() {
            return true;
        }
    }

    private static final class JavaCustomRule implements ConfigurationRule {

        private final RuleDependency dependency;

        private JavaCustomRule(RuleDependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public String id() {
            return "java.custom-rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            if (!dependency.isReady()
                    || !Boolean.TRUE.equals(environment.getProperty("sample.java-rule-enabled", Boolean.class))) {
                return Optional.empty();
            }
            return Optional.of(new ConfigurationViolation(id(), "Java custom rule rejected the configuration"));
        }
    }
}
