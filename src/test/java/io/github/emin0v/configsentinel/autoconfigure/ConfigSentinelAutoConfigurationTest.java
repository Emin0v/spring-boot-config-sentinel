package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

class ConfigSentinelAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigSentinelAutoConfiguration.class));

    @Test
    void loadsWithNoConfigurationRules() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ConfigurationRuleEngine.class);
            assertThat(context.getBean(ConfigurationRuleEngine.class)
                    .evaluate(context.getEnvironment())).isEmpty();
        });
    }

    @Test
    void discoversRulesWithoutEvaluatingThemAtStartup() {
        contextRunner.withBean(ConfigurationRule.class, StartupFailingRule::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ConfigurationRuleEngine.class);
                });
    }

    private static final class StartupFailingRule implements ConfigurationRule {

        @Override
        public String id() {
            return "startup.failing.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            throw new AssertionError("Rule must not be evaluated during startup");
        }
    }
}
