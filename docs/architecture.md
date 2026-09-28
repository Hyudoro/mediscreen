# Architecture

How the services fit together, how they are configured, and the rules the build
enforces. The README covers what each service does and how to run it.

```
browser ──▶ frontend-service :8080   (web interface)
                  │  server-side HTTP
                  ▼
            gateway-service  :9000   (entry point for REST clients)
                  │
      ┌───────────┼────────────┐
      ▼           ▼            ▼
 patient-svc   note-svc   assessment-svc
  :8081         :8082        :8083
      │           │            │
      ▼           ▼            └──▶ reads patient + note through the gateway
 postgres:18   mongo:8.3
```

assessment-service is not built yet.

## 1. Deployment

For now the services run on the host with the dev profile, and
docker-compose.yml starts only the two databases, on 127.0.0.1.

The target layout is one image and one container per service, on one Compose
network. Only the front and the gateway publish a host port, so the gateway is
the only way in to patient data:

| Container          | Image                         | Host port | Container port |
|--------------------|-------------------------------|-----------|----------------|
| frontend-service   | eclipse-temurin:25-jre-alpine | 8080      | 8080           |
| gateway-service    | eclipse-temurin:25-jre-alpine | 9000      | 9000           |
| patient-service    | eclipse-temurin:25-jre-alpine | none      | 8081           |
| note-service       | eclipse-temurin:25-jre-alpine | none      | 8082           |
| assessment-service | eclipse-temurin:25-jre-alpine | none      | 8083           |
| postgres           | postgres:18-alpine            | none      | 5432           |
| mongo              | mongo:8.3                     | none      | 27017          |

## 2. Services

patient-service: Flyway creates the schema and Hibernate only validates it, so
an entity that does not match the schema stops the startup. The four test
patients are loaded only under the dev and docker profiles.

note-service: the mongo container loads the test notes from
note-service/mongo-init when its data directory is empty. The service creates
the patientId index itself and does not write Spring Data's _class field, so
every note has the same five fields. Notes are never trimmed or truncated.

assessment-service will hold rules and no data: it reads patients and notes
from the other two services.

gateway-service: routes live under spring.cloud.gateway.server.webflux.routes.
The older spring.cloud.gateway.routes is silently ignored. The gateway checks
no credentials: it forwards the Authorization header to the service behind it.

## 3. Calls between services

Synchronous HTTP, with RestClient behind @HttpExchange interfaces:

1. The front calls the gateway to build each page, with its own service
   account, never with the user's credentials.
2. assessment-service will call the gateway, which forwards to patient-service
   and note-service. Going through the gateway keeps the routing in one place.

## 4. Correlation id and logs

The gateway keeps the X-Correlation-Id header a request comes with, or creates
one, forwards it and returns it in the response, so a caller can quote it when
reporting a problem. The services behind the gateway do not log it.

Every service logs one ECS JSON document per line on stdout, with Spring Boot's
structured logging.

## 5. Configuration

Each secret is a file, read as a configuration tree: the file name becomes the
property name. The dev profile reads secrets/ from the working directory or its
parent. The docker profile reads /run/secrets, where Compose mounts them.
application.yml uses each secret by name, with no default, so a missing secret
stops the startup:

```yaml
spring:
  datasource:
    password: ${postgres-password}
```

| File                  | Read by                       |
|-----------------------|-------------------------------|
| postgres-password     | patient-service, postgres     |
| mongo-password        | note-service, mongo           |
| service-password-hash | patient-service, note-service |
| service-password      | frontend-service              |
| ui-password-hash      | frontend-service              |

## 6. Layering

patient-service and note-service share the same packages under
com.mediscreen.<service>: controller, service, repository, domain, dto,
exception and config. frontend-service has client, controller, dto and config.

In the two back services, ArchUnit fails the build when:

- a layer calls upwards: controllers call services, services call repositories
- a controller uses a repository
- a controller depends on an entity or a document
- packages depend on each other in a cycle

Also followed, though the build does not check them: constructor injection,
@Valid on every incoming DTO, DTOs as records with a static from(...) factory,
no Lombok or MapStruct, and errors as ProblemDetail (RFC 9457) except the 401
without credentials. The front's two form objects are classes, because Thymeleaf
binds forms through setters.
