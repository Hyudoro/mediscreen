package com.mediscreen.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("The risk level")
class RiskLevelTest {

    private static RiskLevel levelFor(int triggerCount, int age, Gender gender) {
        return RiskLevel.of(new RiskFactors(triggerCount, age, gender));
    }

    @Test
    @DisplayName("is written as on the client's sprint 3 sheet")
    void usesTheSheetsWords() {
        assertThat(RiskLevel.values())
                .extracting(RiskLevel::label)
                .containsExactly("None", "Borderline", "InDanger", "EarlyOnset");
    }

    @Nested
    @DisplayName("from 30, for men and women alike")
    class From30 {

        @ParameterizedTest(name = "{0} triggers: {1}")
        @CsvSource({
            "0, NONE",
            "1, NONE",
            "2, BORDERLINE",
            "5, BORDERLINE",
            "6, IN_DANGER",
            "7, IN_DANGER",
            "8, EARLY_ONSET",
            "11, EARLY_ONSET"
        })
        void from30(int triggerCount, RiskLevel expected) {
            assertThat(levelFor(triggerCount, 45, Gender.MALE)).isEqualTo(expected);
            assertThat(levelFor(triggerCount, 45, Gender.FEMALE)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("for a man under 30")
    class ManUnder30 {

        @ParameterizedTest(name = "{0} triggers: {1}")
        @CsvSource({"0, NONE", "2, NONE", "3, IN_DANGER", "4, IN_DANGER", "5, EARLY_ONSET", "11, EARLY_ONSET"})
        void manUnder30(int triggerCount, RiskLevel expected) {
            assertThat(levelFor(triggerCount, 25, Gender.MALE)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("for a woman under 30")
    class WomanUnder30 {

        @ParameterizedTest(name = "{0} triggers: {1}")
        @CsvSource({"0, NONE", "3, NONE", "4, IN_DANGER", "6, IN_DANGER", "7, EARLY_ONSET", "11, EARLY_ONSET"})
        void womanUnder30(int triggerCount, RiskLevel expected) {
            assertThat(levelFor(triggerCount, 25, Gender.FEMALE)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("at the age of 30")
    class At30 {

        @Test
        @DisplayName("follows the over-30 rules, and the under-30 rules the year before")
        void followsTheOver30Rules() {
            assertThat(levelFor(3, 29, Gender.MALE)).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(levelFor(3, 30, Gender.MALE)).isEqualTo(RiskLevel.BORDERLINE);
            assertThat(levelFor(2, 29, Gender.FEMALE)).isEqualTo(RiskLevel.NONE);
            assertThat(levelFor(2, 30, Gender.FEMALE)).isEqualTo(RiskLevel.BORDERLINE);
        }
    }
}
