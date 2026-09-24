# spring-boot-config-sentinel

Fail fast on unsafe Spring Boot configuration before it reaches production.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## Why

Risky settings can reach production unnoticed because they are valid Spring Boot configuration. This starter is intended to make a small set of high-impact mistakes visible during application startup.

## Example

The planned v0.1.0 release will detect unsafe configuration such as:

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

Detection and enforcement are not implemented yet.

## Status

Early development. v0.1.0 is in progress.

## MVP scope

Planned for v0.1.0:

- a small configuration rule contract and deterministic evaluation;
- checks for selected JPA, Actuator, logging, and error-handling settings;
- profile-aware fail or warn behavior;
- user-defined forbidden property/value checks;
- startup diagnostics and integration tests.

## Non-goals

This project is not a secret manager, full security scanner, configuration server, or replacement for Spring Boot's configuration property validation.

## Development

The wrapper-based build command will be documented with the initial Java scaffold.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
