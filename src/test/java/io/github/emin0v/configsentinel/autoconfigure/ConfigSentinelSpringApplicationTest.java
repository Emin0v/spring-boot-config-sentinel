package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import io.github.emin0v.configsentinel.rule.ConfigurationViolation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@ExtendWith(OutputCaptureExtension.class)
class ConfigSentinelSpringApplicationTest {

    private static final String SHOW_SQL_VIOLATION =
            "[jpa.show-sql] spring.jpa.show-sql is set to 'true'";

    @ParameterizedTest
    @ValueSource(strings = {"prod", "production"})
    void failsForDefaultTargetProfiles(String profile) {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--spring.profiles.active=" + profile,
                "--spring.jpa.show-sql=true"));

        assertThat(rootCause(failure)).hasMessageContaining(SHOW_SQL_VIOLATION);
    }

    @Test
    void doesNotEnforceBuiltInRulesForDevProfile() {
        try (ConfigurableApplicationContext context = run(
                TestApplication.class,
                "--spring.profiles.active=dev",
                "--spring.jpa.show-sql=true")) {
            assertThat(context.isActive()).isTrue();
        }
    }

    @Test
    void respectsDisabledFlagDuringEarlyValidation() {
        try (ConfigurableApplicationContext context = run(
                TestApplication.class,
                "--spring.profiles.active=prod",
                "--spring.jpa.show-sql=true",
                "--config-sentinel.enabled=false")) {
            assertThat(context.isActive()).isTrue();
        }
    }

    @Test
    void usesCustomTargetProfile() {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--spring.profiles.active=stage",
                "--spring.jpa.show-sql=true",
                "--config-sentinel.profiles[0]=stage"));

        assertThat(rootCause(failure)).hasMessageContaining(SHOW_SQL_VIOLATION);

        try (ConfigurableApplicationContext context = run(
                TestApplication.class,
                "--spring.profiles.active=prod",
                "--spring.jpa.show-sql=true",
                "--config-sentinel.profiles[0]=stage")) {
            assertThat(context.isActive()).isTrue();
        }
    }

    @Test
    void warnsOnceAndAllowsStartup(CapturedOutput output) {
        try (ConfigurableApplicationContext context = run(
                TestApplication.class,
                "--spring.profiles.active=prod",
                "--spring.jpa.show-sql=true",
                "--config-sentinel.action=WARN")) {
            assertThat(context.isActive()).isTrue();
        }

        assertThat(output).containsOnlyOnce(SHOW_SQL_VIOLATION);
    }

    @Test
    void safeConfigurationStartsWithoutWarnings(CapturedOutput output) {
        try (ConfigurableApplicationContext context = run(
                TestApplication.class,
                "--spring.profiles.active=prod")) {
            assertThat(context.isActive()).isTrue();
        }

        assertThat(output).doesNotContain("Config Sentinel found unsafe configuration:");
    }

    @Test
    void reportsAllBuiltInViolationsInStableOrder() {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--spring.profiles.active=prod",
                "--spring.jpa.show-sql=true",
                "--management.endpoints.web.exposure.include=*"));

        String message = rootCause(failure).getMessage();
        assertThat(message)
                .contains("[actuator.web-exposure] "
                        + "management.endpoints.web.exposure.include exposes all endpoints")
                .contains(SHOW_SQL_VIOLATION);
        assertThat(message.indexOf("[actuator.web-exposure]"))
                .isLessThan(message.indexOf("[jpa.show-sql]"));
    }

    @Test
    void loadsProfileSpecificConfigurationBeforeEarlyValidation() {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--spring.config.location=classpath:/profile-config/"));

        assertThat(rootCause(failure)).hasMessageContaining(SHOW_SQL_VIOLATION);
    }

    @Test
    void detectsUnsafePropertyAddedAfterEnvironmentProcessing() {
        Throwable failure = catchThrowable(() -> run(
                LatePropertySourceApplication.class,
                "--spring.profiles.active=prod"));

        assertThat(rootCause(failure)).hasMessageContaining(SHOW_SQL_VIOLATION);
    }

    @Test
    void evaluatesBeanBackedCustomRuleInLatePhase() {
        Throwable failure = catchThrowable(() -> run(
                CustomRuleApplication.class,
                "--spring.profiles.active=prod"));

        assertThat(rootCause(failure))
                .hasMessageContaining("[custom.rule] Custom dependency is ready");
    }

    @Test
    void rejectsCustomRuleIdThatDuplicatesBuiltInRule() {
        Throwable failure = catchThrowable(() -> run(
                DuplicateRuleApplication.class,
                "--spring.profiles.active=prod"));

        assertThat(rootCause(failure))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate rule identifier: jpa.show-sql");
    }

    @Test
    void failsWithGlobalLazyInitialization() {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--spring.main.lazy-initialization=true",
                "--spring.profiles.active=prod",
                "--spring.jpa.show-sql=true"));

        assertThat(rootCause(failure)).hasMessageContaining(SHOW_SQL_VIOLATION);
    }

    @Test
    void validatesConfigurationDuringEarlyProcessing() {
        Throwable failure = catchThrowable(() -> run(
                TestApplication.class,
                "--config-sentinel.profiles[0]= "));

        assertThat(rootCause(failure))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("config-sentinel.profiles must not contain blank profiles");
    }

    private static ConfigurableApplicationContext run(Class<?> source, String... arguments) {
        SpringApplication application = new SpringApplication(source);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(arguments);
    }

    private static Throwable rootCause(Throwable failure) {
        assertThat(failure).isNotNull();
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(ConfigSentinelAutoConfiguration.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:/late-config.properties")
    @ImportAutoConfiguration(ConfigSentinelAutoConfiguration.class)
    static class LatePropertySourceApplication {
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(ConfigSentinelAutoConfiguration.class)
    static class CustomRuleApplication {

        @Bean
        RuleDependency ruleDependency() {
            return new RuleDependency();
        }

        @Bean
        ConfigurationRule customRule(RuleDependency dependency) {
            return new BeanBackedRule(dependency);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(ConfigSentinelAutoConfiguration.class)
    static class DuplicateRuleApplication {

        @Bean
        ConfigurationRule duplicateRule() {
            return new DuplicateRule();
        }
    }

    static final class RuleDependency {

        boolean isReady() {
            return true;
        }
    }

    static final class BeanBackedRule implements ConfigurationRule {

        private final RuleDependency dependency;

        BeanBackedRule(RuleDependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public String id() {
            return "custom.rule";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            if (!dependency.isReady()) {
                return Optional.empty();
            }
            return Optional.of(new ConfigurationViolation(id(), "Custom dependency is ready"));
        }
    }

    static final class DuplicateRule implements ConfigurationRule {

        @Override
        public String id() {
            return "jpa.show-sql";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            return Optional.empty();
        }
    }
}
