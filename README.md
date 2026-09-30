# Mediscreen

Mediscreen helps doctors spot patients at risk of type 2 diabetes. It stores
patient records and consultation notes, and estimates each patient's risk from
their notes, age and sex.

## Services

Five Spring Boot services:

- patient-service: patient records, in PostgreSQL.
- note-service: consultation notes, in MongoDB.
- assessment-service: the risk level, computed from the other two. It has no
  database.
- gateway-service: the single entry point for the API and its documentation,
  on port 9000.
- frontend-service: the web interface, on port 8080. It only talks to the
  gateway.

## Stack

Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3, PostgreSQL 18 with Flyway,
MongoDB 8.3 and Thymeleaf. Tests use JUnit 6, Mockito, AssertJ and
Testcontainers.

## Running it

You need Docker with Compose. Building and testing on the host also needs
JDK 25, and the Maven wrapper is included.

Passwords are read from files in secrets/, which is not committed. Create them:

```sh
mkdir secrets
chmod 700 secrets
openssl rand -base64 24 > secrets/postgres-password
openssl rand -base64 24 > secrets/mongo-password
openssl rand -base64 24 > secrets/service-password
openssl rand -base64 24 > secrets/ui-password
```

The web interface and assessment-service call the other services with
service-password, and you sign in to the web interface with ui-password. Each
one is checked against a BCrypt hash in its own file:

```sh
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 10 mediscreen-service "$(cat secrets/service-password)" \
  | head -n 1 | cut -d: -f2 > secrets/service-password-hash
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 12 practitioner "$(cat secrets/ui-password)" \
  | head -n 1 | cut -d: -f2 > secrets/ui-password-hash
```

htpasswd prints the name and the hash, then an empty line. head and cut keep
only the hash.

The services run as an unprivileged user in their containers, so each file must
be readable by all. The folder stays readable by you only:

```sh
chmod 644 secrets/*
```

A service refuses to start if a file it needs is missing or unreadable.

Build the images and start everything:

```sh
docker compose up --build
```

The first build takes longest, as it downloads Maven and the dependencies.
Compose starts the databases, then each service once the ones it needs are
healthy. On their first start, patient-service loads the client's four test
patients and the mongo container their nine notes.

- Web interface: http://localhost:8080, user practitioner, password in
  secrets/ui-password.
- API: http://localhost:9000.
- API documentation: http://localhost:9000/swagger-ui.html, with a menu at the
  top for each back service. To send requests from it, click Authorize and
  sign in as mediscreen-service, password in secrets/service-password. The
  sign-in covers all three services, and the browser keeps it in its local
  storage until you click Logout.

These are the only two published ports, and only on this machine.

To stop the stack:

```sh
docker compose down
```

Add -v to delete the databases' data as well.

To build and run the tests on the host:

```sh
./mvnw clean verify
```

To build one service only:

```sh
./mvnw -pl note-service verify
```

## API

All requests go through the gateway, under /api/v1, with HTTP Basic
credentials: user mediscreen-service, password in secrets/service-password.

| Request                             | Action                               |
|-------------------------------------|--------------------------------------|
| GET /api/v1/patients                | list patients, one page at a time    |
| GET /api/v1/patients/{id}           | get one patient                      |
| POST /api/v1/patients               | add a patient                        |
| PUT /api/v1/patients/{id}           | update a patient                     |
| GET /api/v1/notes?patientId={id}    | list a patient's notes, newest first |
| POST /api/v1/notes                  | add a note                           |
| GET /api/v1/assessments/{patientId} | get a patient's diabetes risk level  |

Lists take page and size parameters and return content, number, size,
totalElements and totalPages. A note is posted with the patient's last name:

```json
{ "patientId": 1, "patient": "TestNone", "note": "..." }
```

An assessment returns the patient id and one of the four risk levels:

```json
{ "patientId": 1, "riskLevel": "None" }
```

Errors are ProblemDetail (RFC 9457). A request without credentials gets a bare
401. An assessment that cannot read the patient's record or notes gets a 502.

## Data

Patients are in two PostgreSQL tables, patient and address, created by Flyway.
Notes are one MongoDB collection, { _id, patientId, patient, note, createdAt },
indexed on patientId. A note is stored exactly as typed, with no length limit.

Both are in third normal form, as the client's ISO certification requires.
docs/normalisation-3nf.md proves it.

The notes and the risk terms are in French, as the client supplied them.

## Risk levels

assessment-service counts how many of these eleven terms appear in a patient's
notes, each term once however often it appears: Hémoglobine A1C, Microalbumine,
Taille, Poids, Fumeur or Fumeuse, Anormal, Cholestérol, Vertiges, Rechute,
Réaction, Anticorps. The count, the patient's age and sex give the level.
Levels are checked from the most serious down, and the first match wins:

| Level      | Condition                                                      |
|------------|----------------------------------------------------------------|
| EarlyOnset | under 30: at least 5 for men, 7 for women. From 30: at least 8 |
| InDanger   | under 30: at least 3 for men, 4 for women. From 30: at least 6 |
| Borderline | from 30: at least 2                                            |
| None       | any other case                                                 |

A patient is under 30 until their 30th birthday. Matching ignores case and
accents, and accepts each term in the singular, the plural and the feminine:
"Vertige" counts as Vertiges and "anormale" as Anormal, but "fume" does not
count as Fumeur.

## Tests

Unit and slice tests, and integration tests against real PostgreSQL and MongoDB
through Testcontainers. The client's four test patients get the risk levels the
client expects. The build fails below 80% line coverage on the service layer
(JaCoCo), on a layering violation (ArchUnit) and on bad formatting (Spotless).

Mutation testing (PIT) checks the risk rules. It runs on demand:

```sh
./mvnw -pl assessment-service test-compile org.pitest:pitest-maven:mutationCoverage
```

## Green Code

Suggested actions to reduce the project's environmental cost, after two
questions.

What is Green Code for? Software uses energy: servers running, devices rendering
pages, data crossing the network, and the hardware behind all of it. Green Code
means reducing that on purpose: less data, less work per request, and smaller
machines kept longer. It also lowers the hosting bill.

How do you find code that wastes memory? Measure first. Java Flight Recorder and
heap dumps taken under load show which allocations dominate and what survives
garbage collection. The ecoCode rules for SonarQube catch known patterns without
a profiler. Typical causes in an application like this one: loading a whole
table instead of a page, one query per row (N+1), objects created in a loop
instead of once, regular expressions compiled on every call, caches with no size
limit or expiry, and objects serialised into logs on every request.

Suggested actions, most useful first:

- Check the service account's password at BCrypt's lowest cost, 4, in both its
  hash and the services' password encoder. It is a long random value, not one
  a person chose, so a slow hash protects nothing there, and each patient page
  makes five of these checks between the services.
- Keep every collection endpoint paginated, and each page asking only for the
  rows it shows.
- Select only the columns a response needs, with projections.
- Keep the eleven risk terms' patterns compiled once, and the rules checked
  only until the first match.
- Keep an index on every lookup path, as patientId has on notes and patient_id
  on address, and add one for any new lookup.
- Keep the images built in stages, on a JRE base rather than a JDK, with a
  layered jar so that a rebuild ships only what changed.
- Keep the pages server-rendered with hand-written CSS: no framework bundle, no
  third-party fonts.
- Compress responses at the gateway.
- Keep the architecture to what the specification asks for: no service
  registry, no configuration server, no logging stack.
- Keep SonarQube in its own compose file, out of the everyday start.
- In CI, cache the Maven repository and the Testcontainers images, and run on
  push and pull request rather than on a schedule.
- Consider a native image if startup time and idle memory matter more than a
  simple build.
- Consider caching assessment results, if the computation saved outweighs the
  risk of showing a stale level.
- Measure before and after each change, with ecoCode, image sizes and memory
  use.

## Status

Sprints 1 to 3 are done: patient records, notes and the diabetes risk, through
the gateway and the web interface. Each service runs in its own Docker image,
and Compose starts the whole stack. Still to come: a GitHub Actions pipeline.

## More detail

- docs/architecture.md: how the services connect, their configuration, and the
  layering rules.
- docs/normalisation-3nf.md: the third normal form proof.
