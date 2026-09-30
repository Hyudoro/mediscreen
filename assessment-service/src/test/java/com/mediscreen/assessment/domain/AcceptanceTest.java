package com.mediscreen.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediscreen.assessment.ClientTestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The expected levels depend on the date: patient 4 turns 30 in 2032, and from then on seven triggers
 * no longer make them EarlyOnset. The tests are pinned to the day they were written.
 */
@DisplayName("The client's four test patients")
class AcceptanceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    @ParameterizedTest(name = "patient {0}: {1}")
    @DisplayName("get the level the sprint 3 sheet expects")
    @CsvSource({"1, None", "2, Borderline", "3, InDanger", "4, EarlyOnset"})
    void getTheExpectedLevel(long patientId, String expected) {
        var patient = ClientTestData.patient(patientId);

        var factors = RiskFactors.of(patient.notes(), patient.birthDate(), patient.gender(), TODAY);

        assertThat(RiskLevel.of(factors).label()).isEqualTo(expected);
    }
}
