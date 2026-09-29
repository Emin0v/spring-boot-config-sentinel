package io.github.emin0v.configsentinel.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class HibernateInitializationOrderTest {

    private static final String DATABASE_URL =
            "jdbc:h2:mem:config_sentinel_initialization_order;DB_CLOSE_DELAY=-1";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    ConfigSentinelAutoConfiguration.class))
            .withUserConfiguration(JpaTestConfiguration.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
            .withPropertyValues(
                    "spring.datasource.url=" + DATABASE_URL,
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create");

    @Test
    void hibernateCreatesSchemaBeforeEnforcementRejectsContext() throws SQLException {
        assertThat(probeTableExists()).isFalse();

        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootCause(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "[jpa.ddl-auto] spring.jpa.hibernate.ddl-auto is set to 'create'");
        });

        assertThat(probeTableExists()).isTrue();
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

    private static Throwable rootCause(AssertableApplicationContext context) {
        Throwable cause = context.getStartupFailure();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = ProbeEntity.class)
    static class JpaTestConfiguration {
    }

    @Entity
    @Table(name = "sentinel_order_probe")
    static class ProbeEntity {

        @Id
        private Long id;
    }
}
