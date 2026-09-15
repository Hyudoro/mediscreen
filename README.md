# Mediscreen

Mediscreen is a diabetes screening tool. It stores patient records and the notes
doctors write during consultations, then reads those notes to work out how
likely a patient is to develop type 2 diabetes.

It is split into five Spring Boot services, each with its own Docker image.

## The services

- patient-service keeps patient records in PostgreSQL.
- note-service keeps consultation notes in MongoDB.
- assessment-service works out the risk level. It has no database of its own and
  gets what it needs from the other two.
- gateway-service is the single entry point to those three, on port 9000.
- frontend-service is the web interface, Thymeleaf pages on port 8080.

Only the gateway and the web interface publish a port. The three services behind
them and both databases are reachable only on the Docker network.

## Stack

Java 25, Spring Boot 4.0.8, Spring Cloud 2025.1.3. PostgreSQL 18 with Flyway,
MongoDB 8.3. Thymeleaf and hand-written CSS. JUnit 5, Mockito, AssertJ and
Testcontainers for the tests. Images built on eclipse-temurin:25-jre-alpine.

## Running it

You need Docker with the Compose plugin. Maven is not required, the wrapper is in
the repository.

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
```

The fourth file is the BCrypt hash of the third, so it is generated from it.
htpasswd prints a name and a hash separated by a colon, and cut keeps the hash:

```sh
docker run --rm httpd:2.4-alpine \
  htpasswd -nbBC 10 mediscreen "$(cat secrets/service-password)" \
  | cut -d: -f2 > secrets/service-password-hash
```

Nothing starts until the four files exist.

```sh
docker compose up --build
```

The web interface is on http://localhost:8080 and the gateway on
http://localhost:9000, with the OpenAPI page at
http://localhost:9000/swagger-ui.html.

```sh
./mvnw clean verify                     # build and test everything
./mvnw -pl patient-service -am verify   # one service
```


## API

Everything is under /api/v1 and goes through the gateway:

- /api/v1/patients goes to patient-service
- /api/v1/notes goes to note-service
- /api/v1/assessments goes to assessment-service

Lists are paginated and errors come back as ProblemDetail. The three services
behind the gateway require HTTP Basic authentication; the web interface has a
login form and sends the credentials on itself.

## Data

Patients are in PostgreSQL across three tables: gender, patient and address. The
schema is normalised to third normal form, which the client needs for an ISO
certification, and docs/normalisation-3nf.md sets out why it holds. Flyway owns
the schema.

Notes are one Mongo collection, { _id, patientId, patient, note, createdAt },
indexed on patientId. A note is stored as it was typed, line breaks included,
and has no length limit.

The notes and the terms the risk rules look for are in French. That is how the
source data was supplied.

## Risk levels

assessment-service counts how many of eleven medical terms appear in a patient's
notes, each term counting once however often it occurs, and combines that count
with the patient's age and sex. The terms are Hémoglobine A1C, Microalbumine,
Taille, Poids, Fumeur or Fumeuse, Anormal, Cholestérol, Vertiges, Rechute,
Réaction and Anticorps.

| Level      | Condition                                      |
|------------|------------------------------------------------|
| None       | fewer than 2 terms                             |
| Borderline | 2 to 5 terms and over 30                       |
| InDanger   | under 30: 3 for men, 4 for women. Over 30: 6   |
| EarlyOnset | under 30: 5 for men, 7 for women. Over 30: 8   |

The levels are checked from the most serious downwards and the first match wins.

Matching ignores case and accents and works on the stem of the word, so
"Vertige" in a note matches the term "Vertiges". A word that merely starts the
same way does not count: "il fume" is not "Fumeur".

## Tests

Unit and slice tests with Mockito and AssertJ, integration tests against a real
PostgreSQL and a real MongoDB through Testcontainers. JaCoCo fails the build
below 80% line coverage on the service layer and ArchUnit fails it on a layering
violation. GitHub Actions runs all of it on push and pull request.

Four reference patients act as end-to-end checks, one per risk level.

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
  collection, and the foreign keys on patient and address.
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

patient-service, gateway-service and frontend-service are in progress.
note-service and assessment-service have not been started.

## More detail

- docs/architecture.md: deployment layout, how the services call each other, and
  the layering rules.
- docs/normalisation-3nf.md: the normalisation write-up.
