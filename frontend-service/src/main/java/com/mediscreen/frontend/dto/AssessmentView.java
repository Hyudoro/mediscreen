package com.mediscreen.frontend.dto;

/** A patient's diabetes risk as the gateway returns it. */
public record AssessmentView(long patientId, String riskLevel) {

    /** The level as the screens show it, in words rather than as one identifier. */
    public String riskLabel() {
        return switch (riskLevel) {
            case "InDanger" -> "In danger";
            case "EarlyOnset" -> "Early onset";
            default -> riskLevel;
        };
    }
}
