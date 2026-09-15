# Normalisation to 3NF

Abernathy Clinic needs the patient database normalised to third normal form as
part of an ISO certification. This file lists the attributes, the functional
dependencies and the reasoning for each normal form, so the claim can be checked
against the schema instead of taken on trust.

It covers the relational database owned by patient-service. The Mongo collection
owned by note-service is dealt with in section 6.

## 1. Attributes

The patient record as the clinic supplied it:

| Attribute | Example |
|-----------|---------|
| Last name | TestNone |
| First name | Test |
| Birth date | 1966-12-31 |
| Gender | F |
| Address | 1 Brookside St |
| Phone number | 100-222-3333 |

The address and the phone number are optional. The other four are required.

## 2. Schema

```sql
CREATE TABLE gender (
    id    SMALLINT    PRIMARY KEY,
    code  CHAR(1)     NOT NULL UNIQUE,
    label VARCHAR(16) NOT NULL
);

CREATE TABLE patient (
    id           BIGINT       PRIMARY KEY,
    first_name   VARCHAR(64)  NOT NULL,
    last_name    VARCHAR(64)  NOT NULL,
    birth_date   DATE         NOT NULL,
    gender_id    SMALLINT     NOT NULL REFERENCES gender (id),
    phone_number VARCHAR(32)  NULL
);

CREATE TABLE address (
    id         BIGINT       PRIMARY KEY,
    patient_id BIGINT       NOT NULL UNIQUE REFERENCES patient (id),
    street     VARCHAR(128) NOT NULL
);
```

This is indicative. The Flyway migrations are the authoritative version.

## 3. Functional dependencies

gender:

```
id   -> code, label
code -> id, label
```

Two candidate keys, {id} and {code}. id is the primary key.

patient:

```
id -> first_name, last_name, birth_date, gender_id, phone_number
```

One candidate key, {id}. Nothing else is a determinant: two patients can share a
name, a birth date, a gender, and a phone number if they live in the same house.

address:

```
id         -> patient_id, street
patient_id -> id, street
```

Two candidate keys, {id} and {patient_id}. The second is why patient_id carries
a UNIQUE constraint, and that constraint is what makes the relation one to
zero-or-one rather than one to many.

## 4. The normal forms

1NF, atomic values and no repeating groups. Every attribute holds a single
value. There are no comma-separated lists, no phone_1 and phone_2 columns, and
no array types.

street holds the street line as one value, for example "1 Brookside St". That is
atomic for this domain: the clinic records and searches a postal line, never a
house number and a street name separately, so splitting it would invent a
granularity nobody uses.

2NF, no partial dependency on a key. 2NF is only violated when a non-key
attribute depends on part of a composite key. Every primary key here is a single
surrogate column, so no partial dependency is possible and all three tables are
in 2NF.

3NF, no transitive dependency. A non-key attribute must not be determined by
another non-key attribute.

In gender, both code and label are determined by id directly. code -> label does
hold, but code is a candidate key, which 3NF allows.

patient is where the rule does real work. Storing gender as two columns on
patient, say gender_code and gender_label, would create gender_code ->
gender_label: one non-key attribute determining another, which is 2NF but not
3NF. It would also repeat the label on every row and allow two rows to disagree
about what F means. The gender table removes the dependency rather than managing
its consequences.

In address, street is determined by id, and by patient_id because patient_id is
a candidate key, so there is no transitive dependency.

BCNF. Every determinant listed in section 3 is a candidate key, so all three
tables also satisfy BCNF, which is stronger than what was asked for.

## 5. Why the address is a separate table

Keeping street as a nullable column on patient would be legal: id -> street is a
dependency on the key, and 3NF says nothing about nullability. It is split
anyway, for two reasons that are practical rather than formal.

Absence becomes a missing row instead of a NULL. A NULL could equally mean "not
recorded yet", "the patient refused to give one" or "lost during an import",
whereas a missing row states the fact plainly.

If the clinic later records a postcode or a city, those land on a table that
already exists instead of adding more nullable columns to patient.

The phone number stays on patient as a nullable column. It is single-valued, it
has no sub-structure, and separating it would add a join for nothing.

## 6. The note store

note-service stores documents in MongoDB:

```
{ _id, patientId, patient, note, createdAt }
```

Normal forms are a property of the relational model and do not carry over to a
document store, so 3NF does not apply here. The patient field, which holds the
patient's name copied into the note, is a deliberate denormalisation, for three
reasons:

- The clinic's own note format includes it, and it is the name the doctor saw
  when writing the note.
- Notes are read far more often than patients are renamed, and the copy saves a
  call to another service on the common path.
- It is a label recorded at a point in time, not a source of truth. patientId is
  the reference, and the name in patient-service is authoritative.

The ISO requirement concerns the relational database, which is the one described
in sections 2 to 5.
