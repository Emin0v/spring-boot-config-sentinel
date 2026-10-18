# Basic consumer example

This independent Spring Boot application resolves Config Sentinel `0.1.0` from Maven Central.
It uses Java 21 and Spring Boot 3.5.16, and does not substitute the library's source project.

From the repository root, run the consumer tests:

```bash
./gradlew -p examples/basic clean check
```

Run the intentionally unsafe configuration:

```bash
./gradlew -p examples/basic bootRun
```

Startup fails and reports `jpa.show-sql` and `custom.payment.mock-enabled`.
Override both unsafe values to start successfully:

```bash
./gradlew -p examples/basic bootRun --args='--spring.jpa.show-sql=false --payment.mock-enabled=false'
```

This example has no web server. A successful launch closes normally after startup.
