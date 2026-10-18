package example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class BasicApplicationTest {

    @Test
    void publishedStarterRejectsUnsafeExampleConfiguration() {
        assertThatThrownBy(() -> application().run())
                .hasStackTraceContaining("jpa.show-sql")
                .hasStackTraceContaining("custom.payment.mock-enabled");
    }

    @Test
    void exampleStartsWhenUnsafeValuesAreOverridden() {
        try (ConfigurableApplicationContext context = application().run(
                "--spring.jpa.show-sql=false", "--payment.mock-enabled=false")) {
            assertThat(context.isActive()).isTrue();
        }
    }

    private SpringApplication application() {
        SpringApplication application = new SpringApplication(BasicApplication.class);
        application.setRegisterShutdownHook(false);
        return application;
    }
}
