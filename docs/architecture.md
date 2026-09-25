# Architecture

The README covers what each service does and how to run it. This file has the
parts a reader needs when changing something: the deployment layout, how the
services call each other, the configuration, and the rules the build enforces.

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

## 1. Deployment layout

The services do not run in containers yet. They run on the host with the dev
profile, and docker-compose.yml starts the two databases, with their ports
published on 127.0.0.1 only.

The planned layout is one image per service, one container per service and a
single Compose network:

| Container          | Image base                    | Host port | Container port |
|--------------------|-------------------------------|-----------|----------------|
| frontend-service   | eclipse-temurin:25-jre-alpine | 8080      | 8080           |
| gateway-service    | eclipse-temurin:25-jre-alpine | 9000      | 9000           |
| patient-service    | eclipse-temurin:25-jre-alpine | none      | 8081           |
| note-service       | eclipse-temurin:25-jre-alpine | none      | 8082           |
| assessment-service | eclipse-temurin:25-jre-alpine | none      | 8083           |
| postgres           | postgres:18-alpine            | none      | 5432           |
| mongo              | mongo:8.3                     | none      | 27017          |

In that layout only the front and the gateway publish a host port, so the
gateway is the only way in to patient data. docker compose ps shows it: the
PORTS column is empty for every other container.

## 2. Notes on individual services

Only the parts that are not obvious from the README.

patient-service: Flyway owns the DDL and Hibernate runs with ddl-auto set to
validate, so a mismatch between an entity and the schema fails at startup. The
test patients are in a second Flyway location, used only under the dev and
docker profiles.

note-service: the test notes are loaded by a script mounted into
docker-entrypoint-initdb.d, which the mongo container runs only when its data
directory is empty. The service creates the index on patientId itself at
startup, and writes notes without Spring Data's _class field, so every note has
the same fields as a seeded one. Notes keep their line breaks and have no length
limit, so nothing trims, normalises or truncates them on the way in or out.

assessment-service will hold rules and no data. Its answers will be derived from
the other two services, so it will have nothing to persist.

gateway-service: routes are configured under
spring.cloud.gateway.server.webflux.routes, the namespace Spring Cloud 2025.x
reads. The older spring.cloud.gateway.routes form is ignored without an error.
The gateway checks no credentials: it passes the Authorization header on, and
the service behind it checks it.

## 3. How the services call each other

Plain synchronous HTTP, using RestClient behind @HttpExchange interfaces.

There are two hops:

1. frontend-service to gateway-service, server side, to build each page. The
   front calls with its service account, never with the credentials a user
   signed in with.
2. assessment-service to gateway-service, which forwards to patient-service and
   note-service. This hop comes with assessment-service.

The second hop goes back through the gateway instead of calling the two services
directly, so the routing stays in one place.

## 4. Correlation ids and logs

gateway-service keeps the X-Correlation-Id a request comes with, or generates
one when there is none, passes it to the service behind the route and returns it
in the response. The services do not write it in their logs yet.

Every service logs with Spring Boot's structured logging
(logging.structured.format.console set to ecs): one ECS JSON document per line
on stdout.

## 5. Configuration

Secrets are files, read as a configuration tree: every file in the directory
becomes a property named after the file. Under the dev profile the directory is
secrets/, looked for in the working directory and in its parent, so a service
starts from the repository root or from its own module. Under the docker
profile, meant for the containers, it is /run/secrets, where Compose mounts
secret files.

application.yml refers to each secret by that name, with no default, so a
service does not start without its secrets:

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

The README says how to create them.

## 6. Layering

patient-service and note-service use the same packages under
com.mediscreen.<service>: controller, service, repository, domain, dto,
exception and config. frontend-service has client, controller, dto and config.

In those two services, ArchUnit fails the build when:

- a layer calls upwards: controllers call services, services call repositories,
  never the other way
- a controller uses a repository
- a controller depends on an entity or a document
- packages depend on each other in a cycle

The other conventions are kept by review. Injection goes through constructors,
and every incoming DTO carries @Valid. DTOs are records, and the ones built from
an entity or a document have a static from(...) factory. The front's two form
objects are classes, because Thymeleaf binds a form through setters. There is no
Lombok and no MapStruct. The back services answer errors with a ProblemDetail
(RFC 9457), apart from the 401 for a request without credentials.
