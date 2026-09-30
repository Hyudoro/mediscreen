package com.mediscreen.assessment.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;

/**
 * What the risk rules look at: the number of distinct trigger terms in the patient's notes, the
 * patient's age in whole years, and their gender.
 */
public record RiskFactors(int triggerCount, int age, Gender gender) {

    public static RiskFactors of(Collection<String> notes, LocalDate birthDate, Gender gender, LocalDate today) {
        return new RiskFactors(
                TriggerTerm.foundIn(notes).size(),
                Period.between(birthDate, today).getYears(),
                gender);
    }

    /**
     * Whether the under-30 rules apply. The rules say "under 30" and "over 30" and nothing about 30
     * itself. A patient who has turned 30 is no longer under 30, so the over-30 rules apply from the
     * 30th birthday.
     */
    boolean under30() {
        return age < 30;
    }

    /** The minimum number of triggers for a level: for a man under 30, a woman under 30, anyone from 30. */
    int minimum(int manUnder30, int womanUnder30, int from30) {
        if (!under30()) {
            return from30;
        }
        return gender == Gender.MALE ? manUnder30 : womanUnder30;
    }
}
