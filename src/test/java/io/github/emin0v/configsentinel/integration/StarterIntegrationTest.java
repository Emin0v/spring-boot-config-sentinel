package io.github.emin0v.configsentinel.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
class StarterIntegrationTest {

    private static final String SENTINEL_REPORT = "Config Sentinel found unsafe configuration:";
    private static final String SHOW_SQL_VIOLATION =
            "[jpa.show-sql] spring.jpa.show-sql is set to 'true'";
    private static final String CUSTOM_PROPERTY_VIOLATION =
            "[custom.payment.mock-enabled] Property 'payment.mock-enabled' matches a forbidden value";
    private static final String JAVA_RULE_VIOLATION =
            "[java.custom-rule] Java custom rule rejected the configuration";

    @Test
    void startsWithSafeConfiguration(CapturedOutput output) {
        try (ConfigurableApplicationContext context = run("safe")) {
            assertThat(context.isActive()).isTrue();
            assertThat(context.containsBean("configurationEnforcer")).isTrue();
        }

        assertThat(output).doesNotContain(SENTINEL_REPORT);
    }

    @Test
    void discoversBuiltInRuleFromProfileConfig() {
        Throwable failure = catchThrowable(() -> run("built-in"));

        assertThat(rootCause(failure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SHOW_SQL_VIOLATION);
    }

    @Test
    void appliesYamlCustomPropertyRule() {
        Throwable failure = catchThrowable(() -> run("custom-property"));

        assertThat(rootCause(failure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(CUSTOM_PROPERTY_VIOLATION)
                .hasMessageNotContaining("forbidden-setting");
    }

    @Test
    void discoversJavaCustomRuleBean() {
        Throwable failure = catchThrowable(() -> run("java-rule"));

        assertThat(rootCause(failure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(JAVA_RULE_VIOLATION);
    }

    @Test
    void warnsOnceAndStarts(CapturedOutput output) {
        try (ConfigurableApplicationContext context = run("warn")) {
            assertThat(context.isActive()).isTrue();
        }

        assertThat(output).containsOnlyOnce(SHOW_SQL_VIOLATION);
    }

    @Test
    void startsWhenDisabled(CapturedOutput output) {
        try (ConfigurableApplicationContext context = run("disabled")) {
            assertThat(context.isActive()).isTrue();
        }

        assertThat(output).doesNotContain(SENTINEL_REPORT);
    }

    private static ConfigurableApplicationContext run(String configurationName) {
        SpringApplication application = new SpringApplication(ConsumerApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application.run(
                "--spring.config.location=classpath:/starter-integration/",
                "--spring.config.name=" + configurationName);
    }

    private static Throwable rootCause(Throwable failure) {
        assertThat(failure).isNotNull();
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
