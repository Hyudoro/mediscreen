package com.mediscreen.assessment.exception;

/** Thrown when patient-service knows no patient with the id. Mapped to a 404 ProblemDetail. */
public class PatientNotFoundException extends RuntimeException {

    private final long patientId;

    public PatientNotFoundException(long patientId) {
        super("No patient found with id " + patientId);
        this.patientId = patientId;
    }

    public long getPatientId() {
        return patientId;
    }
}
