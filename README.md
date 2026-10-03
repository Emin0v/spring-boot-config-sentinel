# spring-boot-config-sentinel

Detect selected unsafe production configuration during Spring Boot startup.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![CI](https://github.com/Emin0v/spring-boot-config-sentinel/actions/workflows/ci.yml/badge.svg)](https://github.com/Emin0v/spring-boot-config-sentinel/actions/workflows/ci.yml)

## Why

Spring Boot accepts settings that are useful in development but risky in production. This starter checks a focused set
of explicitly configured properties when selected profiles are active, then either rejects startup or reports warnings.

It is not a secret manager, full security scanner, configuration server, or replacement for Spring Boot configuration
property validation.

Release coordinate:

```text
io.github.emin0v:spring-boot-config-sentinel:0.1.0
```

## What it checks

| Rule ID | Property | Unsafe configured value(s) |
| --- | --- | --- |
| `jpa.ddl-auto` | `spring.jpa.hibernate.ddl-auto` | `update`, `create`, `create-drop`, `create-only`, `drop` |
| `jpa.show-sql` | `spring.jpa.show-sql` | `true` |
| `actuator.web-exposure` | `management.endpoints.web.exposure.include` | A list containing `*` |
| `server.stacktrace` | `server.error.include-stacktrace` | `always` |
| `logging.root-level` | `logging.level.root` | `DEBUG`, `TRACE` |

String values in the built-in rules are trimmed and matched without regard to case. Sentinel checks values present in
the Spring `Environment`; it does not infer framework defaults that are not exposed as configured properties.

## How it works

Sentinel runs only when `config-sentinel.enabled` is `true` and an active profile matches a configured target profile.
`FAIL` rejects startup with a deterministic report containing every violation. `WARN` logs the same report once and
allows startup.

In `FAIL` mode, built-in and declarative custom rules available through ConfigData run during environment processing.
If startup reaches the later bean phase, all Spring-managed rules run there as well. Java `ConfigurationRule` beans run
only in that later phase because they may depend on other beans.

## Configuration

The default configuration is:

```yaml
config-sentinel:
  enabled: true
  profiles:
    - prod
    - production
  action: FAIL
```

| Property | Default | Description |
| --- | --- | --- |
| `config-sentinel.enabled` | `true` | Enables or disables evaluation. |
| `config-sentinel.profiles` | `prod`, `production` | Active profiles that trigger evaluation. At least one non-blank profile is required. |
| `config-sentinel.action` | `FAIL` | `FAIL` rejects startup; `WARN` logs violations and allows startup. |
| `config-sentinel.custom-rules` | Empty | Declarative forbidden-value rules. |
| `config-sentinel.custom-rules[].property` | None | Required property name for a custom rule. |
| `config-sentinel.custom-rules[].forbidden-values` | None | Required non-empty list of forbidden values. |

Profiles use Spring's effective profile matching. If no target profile matches, no rules are evaluated.

## Custom property rules

Declarative rules can reject exact values for application properties:

```yaml
payment:
  mock-enabled: true

config-sentinel:
  custom-rules:
    - property: payment.mock-enabled
      forbidden-values:
        - "true"
```

Values are compared as exact, case-sensitive strings; they are not trimmed or normalized. A missing property passes.
Diagnostics identify the property without including its configured or forbidden values. The generated rule ID is the
property prefixed with `custom.`, such as `custom.payment.mock-enabled`. Duplicate rule IDs are rejected.

## Java custom rules

Register a `ConfigurationRule` as a Spring bean:

```java
@Bean
ConfigurationRule fakeSmsProviderRule() {
    return new ConfigurationRule() {
        @Override
        public String id() {
            return "feature.fake-sms-provider";
        }

        @Override
        public Optional<ConfigurationViolation> evaluate(Environment environment) {
            if (!Boolean.TRUE.equals(
                    environment.getProperty("feature.fake-sms-provider", Boolean.class))) {
                return Optional.empty();
            }
            return Optional.of(new ConfigurationViolation(id(), "Fake SMS provider is enabled"));
        }
    };
}
```

Java rules participate in profile and action handling, and run in the late Spring bean phase. Rule IDs must be non-blank
and unique across built-in, declarative, and Java rules.

## Example

The [basic sample](examples/basic) contains a minimal `@SpringBootApplication` and configuration that intentionally
violates one built-in rule and one custom property rule. Set both example values to `false` for a safe startup.

## Lifecycle guarantees and limitations

With `SpringApplication` in `FAIL` mode, built-in and declarative rules whose configuration is available during
environment processing run before ordinary singleton initialization. This can prevent early side effects such as
Hibernate DDL when the unsafe property comes from normal ConfigData.

If startup reaches the later bean phase, all registered rules are evaluated there. `WARN` uses only this phase so a
violation is not logged twice. Java rule beans also run late because they may depend on other beans. Properties
introduced after environment processing, including through `@PropertySource`, can only be detected late; Sentinel
cannot prevent bean side effects that occurred before such configuration became visible. Contexts created without
`SpringApplication` also use only the late phase.

## Compatibility

Version `0.1.0` is built and tested with Java 21 and Spring Boot 3.5.16. Compatibility with other Spring Boot release
lines is not claimed.

## Development

Java 21 is required. Run the complete build and test suite with:

```bash
./gradlew clean check
```

Generate and verify the Maven publication locally with:

```bash
./gradlew publishToMavenLocal
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
