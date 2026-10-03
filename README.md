# spring-boot-config-sentinel

Fail fast on unsafe Spring Boot configuration before it reaches production.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![CI](https://github.com/Emin0v/spring-boot-config-sentinel/actions/workflows/ci.yml/badge.svg)](https://github.com/Emin0v/spring-boot-config-sentinel/actions/workflows/ci.yml)

## Why

Risky settings can reach production unnoticed because they are valid Spring Boot configuration. This starter is intended to make a small set of high-impact mistakes visible during application startup.

## Example

The v0.1.0 rule set detects unsafe configuration such as:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

Configuration Sentinel evaluates the built-in rules after Spring Boot has loaded ConfigData and before it creates the
application context when an active profile matches:

```yaml
config-sentinel:
  enabled: true
  profiles:
    - prod
    - production
  action: FAIL
```

These are the defaults. `FAIL` prevents successful context startup and reports every violation, for example:

```text
Config Sentinel found unsafe configuration:
- [jpa.show-sql] spring.jpa.show-sql is set to 'true'
```

Set `action: WARN` to log the same violations and allow startup, or set `enabled: false` to skip evaluation. Profiles
are matched using Spring's effective profile handling; if none of the configured profiles matches, rules are not
evaluated.

With `SpringApplication` in `FAIL` mode, built-in rules whose configuration is available during environment processing
run before ordinary singleton initialization. All registered rules run in a later phase as well, so properties introduced
after environment processing are still detected. Those late failures prevent successful startup but cannot prevent
earlier bean-initialization side effects. `WARN` mode runs only in the later phase so each violation is reported once.
User-defined `ConfigurationRule` beans also run in the later phase because they may depend on other Spring beans.
Contexts created without `SpringApplication` use only this later phase because Boot's environment post-processors do
not run in that case.

The [basic sample](examples/basic) pairs a minimal `@SpringBootApplication` with production configuration that
intentionally violates one built-in rule and one custom property rule. Set both example values to `false` to see the
same application start with safe configuration.

### Custom property rules

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
Diagnostics identify the property but do not include its configured or forbidden values. Duplicate properties and
generated rule IDs that conflict with Java-defined `ConfigurationRule` beans are rejected.

In `FAIL` mode, declarative rules loaded through ConfigData run during early environment processing and again during
the late validation phase. Properties added later, such as through `@PropertySource`, can only be detected in the late
phase, after some bean initialization may already have happened. `ConfigurationRule` beans always run only in that
late phase.

## Status

Early development. v0.1.0 is in progress.

## MVP scope

Planned for v0.1.0:

- a small configuration rule contract and deterministic evaluation (implemented);
- checks for selected JPA, Actuator, logging, and error-handling settings (implemented);
- profile-aware fail or warn behavior (implemented);
- user-defined forbidden property/value checks (implemented);
- startup diagnostics and integration tests (implemented).

## Non-goals

This project is not a secret manager, full security scanner, configuration server, or replacement for Spring Boot's configuration property validation.

## Development

Java 21 is required. Run the complete build and test suite with:

```bash
./gradlew check
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
