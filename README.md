# Mediscreen

Mediscreen is a diabetes screening tool. It stores patient records and the notes
doctors write during consultations, and will read those notes to work out how
likely a patient is to develop type 2 diabetes.

It is split into five Spring Boot services.

## The services

- patient-service keeps patient records in PostgreSQL.
- note-service keeps consultation notes in MongoDB.
- assessment-service will work out the risk level. It will have no database of
  its own and will get what it needs from the other two. It is not built yet.
- gateway-service, on port 9000, routes requests to those three.
- frontend-service is the web interface, Thymeleaf pages on port 8080. It calls
  the gateway only, never a service behind it.

## Stack

Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3. PostgreSQL 18 with Flyway,
MongoDB 8.3. Thymeleaf and hand-written CSS. JUnit 6, Mockito, AssertJ and
Testcontainers for the tests.

## Running it

You need a JDK 25 and Docker with the Compose plugin. Maven is not required, the
wrapper is in the repository.

The services read their passwords from files under secrets/, which is not
committed. Create the directory first:

```sh
mkdir secrets
chmod 700 secrets
```

Then one random password per file:

```sh
openssl rand -base64 24 > secrets/postgres-password
openssl rand -base64 24 > secrets/mongo-password
openssl rand -base64 24 > secrets/service-password
openssl rand -base64 24 > secrets/ui-password
```

service-password is the one the web interface uses to call the services, and
ui-password is the one you sign in to the web interface with. Each is checked
against a BCrypt hash kept in a file of its own. htpasswd prints a name and a
hash separated by a colon, and cut keeps the hash:

```sh
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 10 mediscreen-service "$(cat secrets/service-password)" \
  | cut -d: -f2 > secrets/service-password-hash
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 10 practitioner "$(cat secrets/ui-password)" \
  | cut -d: -f2 > secrets/ui-password-hash
```

A service does not start if a file it reads is missing.

The services do not run in containers yet: Compose starts the two databases, and
the services run on the host. Start the databases, then build:

```sh
docker compose up -d postgres mongo
./mvnw clean verify
```

The build runs every test and leaves one jar per service in its target
directory. Start each service in a terminal of its own, from the repository
root:

```sh
java -jar patient-service/target/patient-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar note-service/target/note-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
java -jar frontend-service/target/frontend-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

The dev profile reads secrets/, and under it patient-service loads the client's
four test patients. The mongo container loads their nine notes the first time it
starts.

The web interface is on http://localhost:8080: sign in as practitioner, with the
password in secrets/ui-password. The gateway is on http://localhost:9000.
patient-service and note-service describe their API on
http://localhost:8081/swagger-ui.html and http://localhost:8082/swagger-ui.html.

To build and test one service only:

```sh
./mvnw -pl note-service verify
```

## API

Everything goes through the gateway, under /api/v1:

| Request                          | What it does                                            |
|----------------------------------|---------------------------------------------------------|
| GET /api/v1/patients             | lists the patients, one page at a time                  |
| GET /api/v1/patients/{id}        | returns one patient                                     |
| POST /api/v1/patients            | adds a patient                                          |
| PUT /api/v1/patients/{id}        | updates a patient                                       |
| GET /api/v1/notes?patientId={id} | lists a patient's notes, newest first, a page at a time |
| POST /api/v1/notes               | adds a note to a patient's history                      |

Lists take page and size parameters. A note is posted as JSON, with the
patient's last name in the patient field:

```json
{ "patientId": 1, "patient": "TestNone", "note": "..." }
```

Every request needs HTTP Basic credentials: mediscreen-service, with the
password in secrets/service-password. The web interface has its own login form,
and sends those credentials itself when it calls the gateway. Errors come back
as ProblemDetail (RFC 9457), and a request without credentials gets a plain 401.

## Data

Patients are in PostgreSQL, in two tables: patient and address. The schema is
normalised to third normal form, which the client needs for an ISO
certification, and docs/normalisation-3nf.md sets out why it holds. Flyway owns
the schema.

Notes are one Mongo collection, { _id, patientId, patient, note, createdAt },
indexed on patientId. A note is stored as it was typed, line breaks included,
and the application sets no limit on its length.

The notes and the terms the risk rules look for are in French. That is how the
source data was supplied.

## Risk levels

assessment-service is not built yet. It will count how many of eleven medical
terms appear in a patient's notes, each term counting once however often it
occurs, and combine that count with the patient's age and sex. The terms are
Hémoglobine A1C, Microalbumine, Taille, Poids, Fumeur or Fumeuse, Anormal,
Cholestérol, Vertiges, Rechute, Réaction and Anticorps.

| Level      | Condition                                                      |
|------------|----------------------------------------------------------------|
| None       | fewer than 2 terms                                             |
| Borderline | 2 to 5 terms and over 30                                       |
| InDanger   | under 30: at least 3 for men, 4 for women. Over 30: at least 6 |
| EarlyOnset | under 30: at least 5 for men, 7 for women. Over 30: at least 8 |

The levels are checked from the most serious downwards and the first match wins.

Matching ignores case and accents and works on the stem of the word, so
"Vertige" in a note matches the term "Vertiges". A word that merely starts the
same way does not count: "il fume" is not "Fumeur".

## Tests

Unit and slice tests with Mockito and AssertJ, and integration tests against a
real PostgreSQL and a real MongoDB through Testcontainers. The build fails below
80% line coverage on the service layer (JaCoCo), on a layering violation
(ArchUnit) and on a formatting error (Spotless).

## Green code

The brief asks for suggested actions to apply Green Code to this project, not for
a report of work already done, so this section is a list of recommendations. Two
questions come with it and are answered first.

What Green Code is for. Software has a physical cost: the electricity a server
draws while it runs, the electricity the client device draws while it renders a
page, the data moved between the two, and the hardware that has to exist to run
it. Green Code is the practice of keeping that cost down on purpose, by moving
less data, doing less work per request, and running on smaller machines for
longer. The environmental argument and the hosting bill point the same way, which
is what makes it worth doing.

How to find code that wastes memory. Measure before changing anything. Java Flight
Recorder and a heap dump taken under load show which allocations dominate and
which objects survive collection; an allocation profile points at the method, not
at a guess. Static analysis with the ecoCode rule set catches the patterns that do
not need a profiler. In an application shaped like this one, the recurring causes
are a repository call that loads a whole table where a page would do, an N+1 query
behind a lazy association, entities carried out to the controller instead of a
DTO, objects allocated inside a loop that could be built once, a regular
expression compiled on every call, a cache with no bound or expiry, and logging
that serialises an object at INFO on a path called on every request.

Suggested actions, most worthwhile first:

- Paginate every endpoint that returns a collection, and have the patient list
  page ask for the page it displays rather than the whole table.
- Return DTOs from controllers and select only the columns a DTO needs, so a
  listing does not carry fields no page shows.
- Compile the eleven search patterns once and stop the risk rules at the first
  match, rather than scanning a patient's notes once per term.
- Index the access paths that are actually used: patientId on the notes
  collection, and patient_id on address.
- Build the images in stages, copy the result into a JRE image rather than a JDK
  one, and keep the layered jar so a rebuild ships changed layers only.
- Serve the pages from the server with hand-written CSS, no framework bundle and
  no web font fetched from a third party.
- Compress responses at the gateway.
- Keep the architecture at what the specification asks for: five services and two
  databases, no service registry, no configuration server, no logging stack. Every
  container that does not exist costs nothing to run.
- Keep the analysis tooling in its own compose file so the everyday start does not
  run SonarQube alongside the application.
- Cache the Maven repository and reuse Testcontainers images in CI, and run the
  pipeline on push and pull request rather than on a schedule.
- Consider a native image if startup time and idle memory matter more than a
  simple build and an easy debugging story.
- Consider caching assessment results, weighing the recomputation saved against
  serving a level that has gone stale.
- Measure before and after each of these, with ecoCode and with image size and
  memory figures, so the list stops being a matter of opinion.

## Status

Sprints 1 and 2 are done: patient records and consultation notes, through the
gateway and the web interface. Next come assessment-service, one Docker image per
service with the whole stack in Compose, and a GitHub Actions pipeline.

## More detail

- docs/architecture.md: how the services fit together and call each other, how
  they are configured, and the layering rules.
- docs/normalisation-3nf.md: the proof that the patient schema is in third
  normal form.
