package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ConfigSentinelAutoConfiguration {

    @Bean
    JpaDdlAutoRule jpaDdlAutoRule() {
        return new JpaDdlAutoRule();
    }

    @Bean
    JpaShowSqlRule jpaShowSqlRule() {
        return new JpaShowSqlRule();
    }

    @Bean
    ActuatorExposureRule actuatorExposureRule() {
        return new ActuatorExposureRule();
    }

    @Bean
    StacktraceExposureRule stacktraceExposureRule() {
        return new StacktraceExposureRule();
    }

    @Bean
    RootLoggingRule rootLoggingRule() {
        return new RootLoggingRule();
    }

    @Bean
    ConfigurationRuleEngine configurationRuleEngine(List<ConfigurationRule> rules) {
        return new ConfigurationRuleEngine(rules);
    }
}
