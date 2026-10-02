package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

class ConfigSentinelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigSentinelAutoConfiguration.class));

    @Test
    void registersBuiltInRules() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigurationRuleEngine.class);
            assertThat(context).hasSingleBean(ConfigurationEnforcer.class);
            assertThat(context).hasSingleBean(ConfigSentinelProperties.class);
            ConfigSentinelProperties properties = context.getBean(ConfigSentinelProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getProfiles()).containsExactly("prod", "production");
            assertThat(properties.getAction()).isEqualTo(ConfigSentinelProperties.Action.FAIL);
            assertThat(properties.getCustomRules()).isEmpty();
            Map<String, ConfigurationRule> rules = context.getBeansOfType(ConfigurationRule.class);
            assertThat(rules.values())
                    .hasSize(5)
                    .extracting(ConfigurationRule::id)
                    .containsExactlyInAnyOrder(
                            "jpa.ddl-auto",
                            "jpa.show-sql",
                            "actuator.web-exposure",
                            "server.stacktrace",
                            "logging.root-level");
            assertThat(context.getBean(ConfigurationRuleEngine.class)
                    .evaluate(context.getEnvironment())).isEmpty();
        });
    }

    @Test
    void doesNotEvaluateRulesOnStartup() {
        contextRunner.withBean(ConfigurationRule.class, ThrowingRule::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ConfigurationRuleEngine.class);
                });
    }

    @Test
    void bindsIndexedCustomRules() {
        contextRunner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "payment.mock-enabled=false",
                        "config-sentinel.custom-rules[0].property=payment.mock-enabled",
                        "config-sentinel.custom-rules[0].forbidden-values[0]=true",
                        "config-sentinel.custom-rules[0].forbidden-values[1]=mock")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ConfigSentinelProperties.CustomRule rule = context.getBean(ConfigSentinelProperties.class)
                            .getCustomRules()
                            .getFirst();
                    assertThat(rule.getProperty()).isEqualTo("payment.mock-enabled");
                    assertThat(rule.getForbiddenValues()).containsExactly("true", "mock");
                });
    }

    private static final class ThrowingRule implements ConfigurationRule {

        @Override
        public String id() {
            return "test.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            throw new AssertionError("Rule must not be evaluated during startup");
        }
    }
}
