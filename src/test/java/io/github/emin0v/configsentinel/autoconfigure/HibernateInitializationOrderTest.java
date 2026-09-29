package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class HibernateInitializationOrderTest {

    private static final String DATABASE_URL =
            "jdbc:h2:mem:config_sentinel_initialization_order;DB_CLOSE_DELAY=-1";

    @Test
    void rejectsConfigurationBeforeHibernateCreatesSchema() throws SQLException {
        assertThat(probeTableExists()).isFalse();

        Throwable failure = catchThrowable(() -> application().run(
                "--spring.profiles.active=prod",
                "--spring.datasource.url=" + DATABASE_URL,
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create"));

        assertThat(failure).isNotNull();
        assertThat(rootCause(failure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "[jpa.ddl-auto] spring.jpa.hibernate.ddl-auto is set to 'create'");
        assertThat(probeTableExists()).isFalse();
    }

    private static SpringApplication application() {
        SpringApplication application = new SpringApplication(JpaTestConfiguration.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.setWebApplicationType(WebApplicationType.NONE);
        return application;
    }

    private static boolean probeTableExists() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        select count(*)
                        from information_schema.tables
                        where table_name = 'SENTINEL_ORDER_PROBE'
                        """)) {
            result.next();
            return result.getInt(1) == 1;
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = ProbeEntity.class)
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        ConfigSentinelAutoConfiguration.class
    })
    static class JpaTestConfiguration {
    }

    @Entity
    @Table(name = "sentinel_order_probe")
    static class ProbeEntity {

        @Id
        private Long id;
    }
}
