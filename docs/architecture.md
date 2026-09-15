# Architecture

The README covers what each service does and how to run the stack. This file has
the parts a reader needs when changing something: the deployment layout, how the
services call each other, and the rules the build enforces.

```
browser ──▶ frontend-service :8080   (published, Thymeleaf UI)
                  │  server-side HTTP
                  ▼
            gateway-service  :9000   (published, REST clients and OpenAPI)
                  │
      ┌───────────┼────────────┐
      ▼           ▼            ▼
 patient-svc   note-svc   assessment-svc   (no host port, internal only)
  :8081         :8082        :8083
      │           │            │
      ▼           ▼            └──▶ reads patient + note through the gateway
 postgres:18   mongo:8.3
```

## 1. Deployment layout

One image per service, one container per service, a single Compose network.

| Container | Image base | Host port | Container port |
|-----------|------------|-----------|----------------|
| frontend-service | eclipse-temurin:25-jre-alpine | 8080 | 8080 |
| gateway-service | eclipse-temurin:25-jre-alpine | 9000 | 9000 |
| patient-service | eclipse-temurin:25-jre-alpine | none | 8081 |
| note-service | eclipse-temurin:25-jre-alpine | none | 8082 |
| assessment-service | eclipse-temurin:25-jre-alpine | none | 8083 |
| postgres | postgres:18-alpine | none | 5432 |
| mongo | mongo:8.3 | none | 27017 |

Only the front and the gateway publish a host port. The other five containers
are reachable on the Compose network only, which is what makes the gateway the
single way in to patient data. It is worth checking after any change to the
compose file, since a stray ports entry undoes it quietly:

```sh
docker compose ps    # PORTS should be empty except for front and gateway
```

## 2. Notes on individual services

Only the parts that are not obvious from the README.

patient-service: Flyway owns the DDL and Hibernate runs with ddl-auto set to
validate, so a mismatch between an entity and the schema fails at startup rather
than later. Sample data loads only under the dev and docker profiles.

note-service: seed data is a JS script mounted into docker-entrypoint-initdb.d.
Notes keep their line breaks and have no length limit, so nothing trims,
normalises or truncates them on the way in or out.

assessment-service: holds rules and no data. Its answers are derived entirely
from the other two services, so there is nothing to persist.

gateway-service: routes are configured under
spring.cloud.gateway.server.webflux.routes. The namespace moved in Spring Cloud
2025.x, and the older spring.cloud.gateway.routes form configures nothing at all
instead of failing, which is an easy hour to lose. The gateway also serves the
combined OpenAPI document.

## 3. How the services call each other

Plain synchronous HTTP, using RestClient behind @HttpExchange interfaces. A
declarative interface keeps the contract as a Java type rather than a URL built
at the call site, and it is easy to stub in a slice test.

There are two hops:

1. frontend-service to gateway-service, server side, once per browser request.
2. assessment-service to gateway-service, which forwards to patient-service and
   note-service.

The second hop deliberately goes back through the gateway instead of calling the
two services directly. Routing stays in one place and assessment-service ends up
knowing nothing about where the others live.

## 4. Correlation ids and logs

gateway-service generates an X-Correlation-Id when the incoming request does not
have one. Each service has a filter that puts it in the MDC, so every log line
for a request carries the same id, and a RestClient interceptor copies it onto
outgoing calls so it survives both hops.

Logs are written with Spring Boot's own structured logging
(logging.structured.format.console set to ecs), which is ECS JSON on stdout,
read with docker compose logs. Tracing a request end to end is a grep on the id.

## 5. Configuration

Compose mounts the secret files under /run/secrets and Spring reads that
directory as a configuration tree:

```yaml
spring:
  config:
    import: "optional:configtree:/run/secrets/"
```

A file mounted at /run/secrets/spring.datasource.password is bound to the
property spring.datasource.password. There are no placeholders and no defaults,
so a service does not start without its secrets. The README says how to create
them.

## 6. Layering

Every service uses the same packages under com.mediscreen.<service>:

```
controller / service / repository / domain / dto / mapper / exception / config
```

ArchUnit checks these rather than leaving them to review:

- Entities do not leave the service layer. Controllers see DTOs only.
- DTOs are records with a static from(...) factory. No Lombok, no MapStruct.
- Constructor injection everywhere, no field injection.
- @Valid on every incoming DTO.
- Errors are returned as ProblemDetail (RFC 9457).
- No package cycles.
