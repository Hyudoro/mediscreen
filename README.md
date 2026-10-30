# Mediscreen

[![CI](https://github.com/Hyudoro/mediscreen/actions/workflows/ci.yml/badge.svg)](https://github.com/Hyudoro/mediscreen/actions/workflows/ci.yml)

Mediscreen helps doctors spot patients at risk of type 2 diabetes. It stores
patient records and consultation notes, and rates each patient's risk from
their notes, age and sex.

## Services

- patient-service: patient records, in PostgreSQL
- note-service: consultation notes, in MongoDB
- assessment-service: the diabetes risk, from the other two
- gateway-service: the entry point to the API and its Swagger UI, on port 9000
- frontend-service: the web interface, on port 8080, a client of the gateway

## Stack

Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3, PostgreSQL 18 with Flyway,
MongoDB 8.3 and Thymeleaf. Tests use JUnit 6, Mockito, AssertJ and
Testcontainers.

## Running it

You need Docker with Compose, and JDK 25 to build on the host.

Create the secrets, in a folder Git ignores:

```sh
mkdir secrets
chmod 700 secrets
openssl rand -base64 24 > secrets/postgres-password
openssl rand -base64 24 > secrets/mongo-password
openssl rand -base64 24 > secrets/service-password
openssl rand -base64 24 > secrets/ui-password
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 10 mediscreen-service "$(cat secrets/service-password)" \
  | head -n 1 | cut -d: -f2 > secrets/service-password-hash
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 12 practitioner "$(cat secrets/ui-password)" \
  | head -n 1 | cut -d: -f2 > secrets/ui-password-hash
chmod 644 secrets/*
```

Start the stack:

```sh
docker compose up --build
```

- Web interface: http://localhost:8080, as practitioner, password in
  secrets/ui-password.
- API: http://localhost:9000, as mediscreen-service, password in
  secrets/service-password.
- Swagger UI: http://localhost:9000/swagger-ui.html. Authorize as
  mediscreen-service, and the browser keeps the sign-in until Logout.

Both ports listen on 127.0.0.1 only.

Stop the stack, with -v to delete the data too:

```sh
docker compose down
```

Build and test on the host, everything or one service:

```sh
./mvnw clean verify
./mvnw -pl note-service verify
```

## API

Every request goes through the gateway, with HTTP Basic as mediscreen-service.

| Request                             | Action                               |
|-------------------------------------|--------------------------------------|
| GET /api/v1/patients                | list patients                        |
| GET /api/v1/patients/{id}           | get a patient                        |
| POST /api/v1/patients               | add a patient                        |
| PUT /api/v1/patients/{id}           | update a patient                     |
| GET /api/v1/notes?patientId={id}    | list a patient's notes, newest first |
| POST /api/v1/notes                  | add a note                           |
| GET /api/v1/assessments/{patientId} | get a patient's diabetes risk        |

Lists are paged with page and size, and return content, number, size,
totalElements and totalPages.

A note, where patient is the patient's last name:

```json
{ "patientId": 1, "patient": "TestNone", "note": "..." }
```

An assessment, where riskLevel is None, Borderline, InDanger or EarlyOnset:

```json
{ "patientId": 1, "riskLevel": "None" }
```

Errors are RFC 9457 problem details, apart from the bare 401 without
credentials. An assessment that cannot read the patient or their notes returns
502.

## Data

PostgreSQL holds the patients in two tables, patient and address, created by
Flyway. MongoDB holds the notes in one collection,
{ _id, patientId, patient, note, createdAt }, indexed on patientId. Notes are
stored as typed, with no length limit. On first start, the databases load the
client's four test patients and their nine notes.

Both are in third normal form, proved in docs/normalisation-3nf.md.

## Risk levels

assessment-service counts the distinct trigger terms in a patient's notes. The
eleven terms, in French: Hémoglobine A1C, Microalbumine, Taille, Poids, Fumeur
or Fumeuse, Anormal, Cholestérol, Vertiges, Rechute, Réaction, Anticorps. They
match as whole words, in the singular, plural or feminine, ignoring case and
accents: "anormale" counts as Anormal, "Vertige" as Vertiges.

A patient gets the first level, from the top, whose minimum they reach:

| Level      | Under 30       | 30 and over   |
|------------|----------------|---------------|
| EarlyOnset | men 5, women 7 | 8             |
| InDanger   | men 3, women 4 | 6             |
| Borderline | never          | 2             |
| None       | anything less  | anything less |

A patient is under 30 until their 30th birthday.

## Tests

Unit and slice tests, and integration tests on real PostgreSQL and MongoDB with
Testcontainers. The client's four test patients get their expected risk levels.
The build fails under 80% line coverage of the service layer (JaCoCo), on a
layering violation (ArchUnit) or on bad formatting (Spotless).

Mutation testing of the risk rules (PIT), on demand:

```sh
./mvnw -pl assessment-service test-compile org.pitest:pitest-maven:mutationCoverage
```

## Green Code

Suggested actions, most useful first:

- Lower the service account's BCrypt cost to 4, in both its hash and the
  services' password encoder. Its password is random, and each patient page
  checks it five times.
- Paginate every new collection endpoint, and index every new lookup.
- Select only the columns a response needs, with projections.
- Compress responses at the gateway.
- Cache the Maven repository and the Testcontainers images in CI.
- Build native images, if startup time and idle memory matter more than a
  simple build.
- Cache assessment results, if a briefly stale risk level is acceptable.
- Measure each change before and after, with the Creedengo rules, image sizes
  and memory use.

## Status

Sprints 1 to 3 are done, the stack runs in Docker, and CI builds and tests
every push to main and every pull request.

## Docs

- docs/architecture.md: how the services connect, their configuration and the
  layering rules
- docs/normalisation-3nf.md: the third normal form proof
