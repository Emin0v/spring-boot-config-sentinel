package io.github.emin0v.configsentinel.autoconfigure;

import io.github.emin0v.configsentinel.rule.ConfigurationRule;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(ConfigSentinelProperties.class)
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

    @Bean
    ConfigurationEnforcer configurationEnforcer(
            ConfigurationRuleEngine ruleEngine, Environment environment, ConfigSentinelProperties properties) {
        return new ConfigurationEnforcer(ruleEngine, environment, properties);
    }
}
