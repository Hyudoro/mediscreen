package com.mediscreen.assessment.dto;

import com.mediscreen.assessment.domain.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

/** A patient's diabetes risk level, written as on the client's sprint 3 sheet. */
public record AssessmentResponse(
        long patientId,

        @Schema(allowableValues = {"None", "Borderline", "InDanger", "EarlyOnset"})
        String riskLevel) {

    public static AssessmentResponse from(long patientId, RiskLevel level) {
        return new AssessmentResponse(patientId, level.label());
    }
}
