package com.mediscreen.assessment.domain;

import java.util.List;
import java.util.function.Predicate;

/** The four diabetes risk levels, each with its rule. */
public enum RiskLevel {
    // Whatever the other three leave, including the counts the specification puts in no level.
    NONE("None", factors -> true),
    BORDERLINE("Borderline", factors -> !factors.under30() && factors.triggerCount() >= 2),
    IN_DANGER("InDanger", factors -> factors.triggerCount() >= factors.minimum(3, 4, 6)),
    EARLY_ONSET("EarlyOnset", factors -> factors.triggerCount() >= factors.minimum(5, 7, 8));

    private static final List<RiskLevel> MOST_SERIOUS_FIRST = List.of(EARLY_ONSET, IN_DANGER, BORDERLINE, NONE);

    private final String label;
    private final Predicate<RiskFactors> rule;

    RiskLevel(String label, Predicate<RiskFactors> rule) {
        this.label = label;
        this.rule = rule;
    }

    /** The level as the client's sprint 3 sheet writes it. */
    public String label() {
        return label;
    }

    /**
     * The most serious level whose rule applies. Checked in that order, each rule needs only its lower
     * bound: over 30, "six or seven" triggers for InDanger means six or more once eight or more is
     * EarlyOnset, and "two to five" for Borderline means two or more once six or more is InDanger.
     */
    public static RiskLevel of(RiskFactors factors) {
        return MOST_SERIOUS_FIRST.stream()
                .filter(level -> level.rule.test(factors))
                .findFirst()
                .orElseThrow();
    }
}
