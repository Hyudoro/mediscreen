package com.mediscreen.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A patient's risk factors")
class RiskFactorsTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1996, Month.SEPTEMBER, 28);

    @Test
    @DisplayName("count the distinct trigger terms across all the notes")
    void countTheDistinctTerms() {
        var factors = RiskFactors.of(
                List.of("Poids stable", "Poids en baisse, audition anormale"),
                BIRTH_DATE,
                Gender.FEMALE,
                LocalDate.of(2026, Month.SEPTEMBER, 28));

        assertThat(factors.triggerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("give the age in whole years, which changes on the birthday")
    void giveTheAgeInWholeYears() {
        assertThat(RiskFactors.of(List.of(), BIRTH_DATE, Gender.MALE, LocalDate.of(2026, Month.SEPTEMBER, 27)))
                .isEqualTo(new RiskFactors(0, 29, Gender.MALE));
        assertThat(RiskFactors.of(List.of(), BIRTH_DATE, Gender.MALE, LocalDate.of(2026, Month.SEPTEMBER, 28)))
                .isEqualTo(new RiskFactors(0, 30, Gender.MALE));
    }
}
