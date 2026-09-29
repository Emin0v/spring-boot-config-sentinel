package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

@ExtendWith(OutputCaptureExtension.class)
class ConfigurationEnforcerTest {

    private static final String UNSAFE_PROPERTY = "spring.jpa.show-sql=true";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigSentinelAutoConfiguration.class));

    @Test
    void skipsEvaluationWhenNoProfileMatches() {
        contextRunner.withPropertyValues(UNSAFE_PROPERTY)
                .withBean(ConfigurationRule.class, ThrowingRule::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void skipsEvaluationForDevProfile() {
        withProfiles("dev").withPropertyValues(UNSAFE_PROPERTY)
                .withBean(ConfigurationRule.class, ThrowingRule::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsForProdProfileByDefault() {
        withProfiles("prod").withPropertyValues(UNSAFE_PROPERTY).run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("[jpa.show-sql] spring.jpa.show-sql is set to 'true'");
        });
    }

    @Test
    void failsForProductionProfileByDefault() {
        withProfiles("production").withPropertyValues(UNSAFE_PROPERTY).run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context)).hasMessageContaining("[jpa.show-sql]");
        });
    }

    @Test
    void enforcesWhenAnyActiveProfileMatches() {
        withProfiles("dev", "prod").withPropertyValues(UNSAFE_PROPERTY).run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context)).hasMessageContaining("[jpa.show-sql]");
        });
    }

    @Test
    void enforcesWhenGlobalLazyInitializationIsEnabled() {
        withProfiles("prod").withInitializer(context -> context.addBeanFactoryPostProcessor(
                        new LazyInitializationBeanFactoryPostProcessor()))
                .withPropertyValues(UNSAFE_PROPERTY)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("[jpa.show-sql] spring.jpa.show-sql is set to 'true'");
                });
    }

    @Test
    void doesNotMatchProfileNamesBySubstring() {
        withProfiles("preprod").withPropertyValues(UNSAFE_PROPERTY)
                .withBean(ConfigurationRule.class, ThrowingRule::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void skipsEvaluationWhenDisabled() {
        withProfiles("prod").withPropertyValues(UNSAFE_PROPERTY, "config-sentinel.enabled=false")
                .withBean(ConfigurationRule.class, ThrowingRule::new)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void warnsAndAllowsStartup(CapturedOutput output) {
        withProfiles("prod").withPropertyValues(UNSAFE_PROPERTY, "config-sentinel.action=warn")
                .run(context -> assertThat(context).hasNotFailed());

        assertThat(output).contains("Config Sentinel found unsafe configuration:")
                .contains("[jpa.show-sql] spring.jpa.show-sql is set to 'true'");
    }

    @Test
    void doesNotWarnWhenConfigurationIsSafe(CapturedOutput output) {
        withProfiles("prod").withPropertyValues("config-sentinel.action=WARN")
                .run(context -> assertThat(context).hasNotFailed());

        assertThat(output).doesNotContain("Config Sentinel found unsafe configuration:");
    }

    @Test
    void reportsAllViolationsInDeterministicOrder() {
        withProfiles("prod").withPropertyValues(
                        "spring.jpa.show-sql=true",
                        "management.endpoints.web.exposure.include=*")
                .run(context -> {
                    assertThat(context).hasFailed();
                    String message = rootCause(context).getMessage();
                    assertThat(message)
                            .contains("[actuator.web-exposure] "
                                    + "management.endpoints.web.exposure.include exposes all endpoints")
                            .contains("[jpa.show-sql] spring.jpa.show-sql is set to 'true'");
                    assertThat(message.indexOf("[actuator.web-exposure]"))
                            .isLessThan(message.indexOf("[jpa.show-sql]"));
                });
    }

    @Test
    void customTargetProfileOverridesDefaults() {
        withProfiles("stage").withPropertyValues(
                        UNSAFE_PROPERTY,
                        "config-sentinel.profiles[0]=stage")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context)).hasMessageContaining("[jpa.show-sql]");
                });

        withProfiles("prod").withPropertyValues(
                        UNSAFE_PROPERTY,
                        "config-sentinel.profiles[0]=stage")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsInvalidAction() {
        withProfiles("prod").withPropertyValues("config-sentinel.action=IGNORE").run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context)).hasMessageContaining("IGNORE");
        });
    }

    @Test
    void rejectsBlankTargetProfile() {
        contextRunner.withPropertyValues("config-sentinel.profiles[0]= ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("config-sentinel.profiles must not contain blank profiles");
        });
    }

    @Test
    void evaluatesUserDefinedRule() {
        withProfiles("prod").withBean(ConfigurationRule.class, CustomRule::new).run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context)).hasMessageContaining("[custom.rule] Custom unsafe configuration");
        });
    }

    @Test
    void evaluatesRulesOnlyOncePerStartup() {
        AtomicInteger evaluations = new AtomicInteger();

        withProfiles("prod").withPropertyValues("config-sentinel.action=WARN")
                .withBean(ConfigurationRule.class, () -> new CountingRule(evaluations))
                .run(context -> assertThat(context).hasNotFailed());

        assertThat(evaluations).hasValue(1);
    }

    private ApplicationContextRunner withProfiles(String... profiles) {
        return contextRunner.withInitializer(context -> {
            ConfigurableEnvironment environment = context.getEnvironment();
            environment.setActiveProfiles(profiles);
        });
    }

    private static Throwable rootCause(AssertableApplicationContext context) {
        Throwable cause = context.getStartupFailure();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static final class ThrowingRule implements ConfigurationRule {

        @Override
        public String id() {
            return "throwing.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            throw new AssertionError("Rule must not be evaluated");
        }
    }

    private static final class CustomRule implements ConfigurationRule {

        @Override
        public String id() {
            return "custom.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            return Optional.of(new ConfigurationViolation(id(), "Custom unsafe configuration"));
        }
    }

    private static final class CountingRule implements ConfigurationRule {

        private final AtomicInteger evaluations;

        private CountingRule(AtomicInteger evaluations) {
            this.evaluations = evaluations;
        }

        @Override
        public String id() {
            return "counting.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            evaluations.incrementAndGet();
            return Optional.empty();
        }
    }
}
