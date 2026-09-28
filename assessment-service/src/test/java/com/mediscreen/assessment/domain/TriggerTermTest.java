package com.mediscreen.assessment.domain;

import static com.mediscreen.assessment.domain.TriggerTerm.ABNORMAL;
import static com.mediscreen.assessment.domain.TriggerTerm.ANTIBODIES;
import static com.mediscreen.assessment.domain.TriggerTerm.CHOLESTEROL;
import static com.mediscreen.assessment.domain.TriggerTerm.HEIGHT;
import static com.mediscreen.assessment.domain.TriggerTerm.HEMOGLOBIN_A1C;
import static com.mediscreen.assessment.domain.TriggerTerm.WEIGHT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Finding the trigger terms")
class TriggerTermTest {

    private static Set<TriggerTerm> found(String... notes) {
        return TriggerTerm.foundIn(List.of(notes));
    }

    @Nested
    @DisplayName("each term")
    class EachTerm {

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @DisplayName("is found as the specification writes it")
        @CsvSource({
            "Hémoglobine A1C, HEMOGLOBIN_A1C",
            "Microalbumine, MICROALBUMIN",
            "Taille, HEIGHT",
            "Poids, WEIGHT",
            "Fumeur, SMOKER",
            "Fumeuse, SMOKER",
            "Anormal, ABNORMAL",
            "Cholestérol, CHOLESTEROL",
            "Vertiges, VERTIGO",
            "Rechute, RELAPSE",
            "Réaction, REACTION",
            "Anticorps, ANTIBODIES"
        })
        void isFoundAsWritten(String word, TriggerTerm term) {
            assertThat(found("Le patient déclare : " + word + " à surveiller")).containsExactly(term);
        }

        @ParameterizedTest(name = "\"{0}\" is {1}")
        @DisplayName("is found in its other forms")
        @CsvSource({
            "Vertige, VERTIGO",
            "anormale, ABNORMAL",
            "anormales, ABNORMAL",
            "anormaux, ABNORMAL",
            "fumeurs, SMOKER",
            "fumeuses, SMOKER",
            "réactions, REACTION",
            "rechutes, RELAPSE",
            "tailles, HEIGHT",
            "microalbumines, MICROALBUMIN",
            "cholestérols, CHOLESTEROL"
        })
        void isFoundInOtherForms(String word, TriggerTerm term) {
            assertThat(found(word)).containsExactly(term);
        }
    }

    @Nested
    @DisplayName("the way a note is typed")
    class Typing {

        @ParameterizedTest(name = "\"{0}\"")
        @DisplayName("does not matter for case or accents")
        @ValueSource(strings = {"cholestérol", "CHOLESTÉROL", "cholesterol", "Cholesterol"})
        void ignoresCaseAndAccents(String word) {
            assertThat(found(word)).containsExactly(CHOLESTEROL);
        }

        @Test
        @DisplayName("finds a two-word term split over two lines")
        void findsATwoWordTermOverALineBreak() {
            assertThat(found("Hémoglobine\nA1C supérieure au niveau recommandé"))
                    .containsExactly(HEMOGLOBIN_A1C);
        }

        @Test
        @DisplayName("finds terms next to punctuation")
        void findsTermsNextToPunctuation() {
            assertThat(found("Taille, Poids (anticorps) d’anormal."))
                    .containsExactlyInAnyOrder(HEIGHT, WEIGHT, ANTIBODIES, ABNORMAL);
        }
    }

    @Nested
    @DisplayName("a word that only looks like a term")
    class LookAlikes {

        @ParameterizedTest(name = "\"{0}\"")
        @DisplayName("is not Fumeur when it is the verb")
        @ValueSource(strings = {"il fume depuis peu", "commencé à fumer", "il a fumé", "ils fument"})
        void theVerbIsNotSmoker(String note) {
            assertThat(found(note)).isEmpty();
        }

        @Test
        @DisplayName("is not a term when the term is only part of it")
        void aTermInsideAnotherWordIsNotFound() {
            assertThat(found("Examen détaillé")).isEmpty();
        }
    }

    @Nested
    @DisplayName("counting")
    class Counting {

        @Test
        @DisplayName("counts a term once however many times a note repeats it")
        void countsATermOnceInANote() {
            assertThat(found("Poids égal ou inférieur au poids recommandé")).containsExactly(WEIGHT);
        }

        @Test
        @DisplayName("counts a term once however many notes contain it")
        void countsATermOnceAcrossNotes() {
            assertThat(found("audition anormale", "crises anormales", "Anormal"))
                    .containsExactly(ABNORMAL);
        }

        @Test
        @DisplayName("finds nothing in a note without a term, or without notes")
        void findsNothing() {
            assertThat(found("Le patient déclare qu'il 'se sent très bien'")).isEmpty();
            assertThat(found()).isEmpty();
        }
    }
}
