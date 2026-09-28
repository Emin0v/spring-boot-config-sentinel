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

Configuration Sentinel evaluates the built-in rules during application context initialization when an active profile
matches:

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

Enforcement runs after ordinary singleton instantiation and before context refresh completes. It prevents successful
startup in `FAIL` mode, but does not guarantee that no earlier bean-initialization side effect has occurred.

## Status

Early development. v0.1.0 is in progress.

## MVP scope

Planned for v0.1.0:

- a small configuration rule contract and deterministic evaluation (implemented);
- checks for selected JPA, Actuator, logging, and error-handling settings (implemented);
- profile-aware fail or warn behavior (implemented);
- user-defined forbidden property/value checks;
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
