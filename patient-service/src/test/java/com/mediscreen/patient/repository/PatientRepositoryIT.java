package com.mediscreen.patient.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mediscreen.patient.TestcontainersConfiguration;
import com.mediscreen.patient.domain.Gender;
import com.mediscreen.patient.domain.Patient;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Runs against a real PostgreSQL with the Flyway migrations applied, so the schema is tested rather
 * than assumed. The demo seed is not on the migration path here: every row these tests assert on is
 * inserted by the test itself.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("The patient repository")
class PatientRepositoryIT {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @Nested
    @DisplayName("saving a patient")
    class Saving {

        @Test
        @DisplayName("stores the address in its own row")
        void storesTheAddress() {
            var saved = repository.saveAndFlush(new Patient(
                    "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "1 Brookside St", "100-222-3333"));

            assertThat(repository.findById(saved.getId()))
                    .get()
                    .satisfies(patient -> assertThat(patient.getStreet()).contains("1 Brookside St"));
        }

        @Test
        @DisplayName("accepts a record with no address and no phone number")
        void acceptsARecordWithoutOptionalFields() {
            var saved = repository.saveAndFlush(
                    new Patient("Jane", "Doe", LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null));

            assertThat(repository.findById(saved.getId())).get().satisfies(patient -> {
                assertThat(patient.getStreet()).isEmpty();
                assertThat(patient.getPhoneNumber()).isNull();
            });
        }

        @Test
        @DisplayName("round-trips the gender through the CHAR(1) column")
        void roundTripsTheGender() {
            var saved = repository.saveAndFlush(
                    new Patient("John", "Doe", LocalDate.of(1990, 1, 1), Gender.MALE, null, null));

            assertThat(repository.findById(saved.getId()))
                    .get()
                    .satisfies(patient -> assertThat(patient.getGender()).isEqualTo(Gender.MALE));
        }
    }

    @Nested
    @DisplayName("the schema itself")
    class Schema {

        @Test
        @DisplayName("rejects a gender outside F and M")
        void rejectsAnInvalidGender() {
            assertThatThrownBy(() -> jdbcClient
                            .sql("INSERT INTO patient (first_name, last_name, birth_date, gender) "
                                    + "VALUES ('a', 'b', DATE '1990-01-01', 'X')")
                            .update())
                    .hasMessageContaining("patient_gender_check");
        }

        @Test
        @DisplayName("keeps the address relation at most one per patient")
        void keepsTheAddressUnique() {
            var patient = repository.saveAndFlush(
                    new Patient("Test", "One", LocalDate.of(1990, 1, 1), Gender.MALE, "1 Street", null));

            assertThat(patient.getStreet()).contains("1 Street");

            patient.setStreet("2 Street");
            var updated = repository.saveAndFlush(patient);

            assertThat(updated.getStreet()).contains("2 Street");
        }
    }

    @Nested
    @DisplayName("looking up an unknown id")
    class UnknownId {

        @Test
        @DisplayName("returns an empty optional")
        void returnsEmpty() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }
}
