-- The four demo patients supplied by the client.
--
-- This location is on spring.flyway.locations only under the dev and docker
-- profiles. A run with no profile gets the schema and nothing else: these rows
-- are fictional and must never reach a production-shaped database.
--
-- Ids are written explicitly because they are client data, not a local
-- numbering: the sprint 2 note sheet (patId) and the sprint 3 expected-risk
-- sheet (PatId) join to these patients by 1 to 4. The identity sequence is
-- restarted afterwards so the first patient created through the API does not
-- collide with them.

INSERT INTO patient (id, first_name, last_name, birth_date, gender, phone_number) VALUES
    (1, 'Test', 'TestNone',       DATE '1966-12-31', 'F', '100-222-3333'),
    (2, 'Test', 'TestBorderline', DATE '1945-06-24', 'M', '200-333-4444'),
    (3, 'Test', 'TestInDanger',   DATE '2004-06-18', 'M', '300-444-5555'),
    (4, 'Test', 'TestEarlyOnset', DATE '2002-06-28', 'F', '400-555-6666');

ALTER TABLE patient ALTER COLUMN id RESTART WITH 5;

INSERT INTO address (patient_id, street) VALUES
    (1, '1 Brookside St'),
    (2, '2 High St'),
    (3, '3 Club Road'),
    (4, '4 Valley Dr');
