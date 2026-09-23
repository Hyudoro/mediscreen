package com.mediscreen.patient.exception;

/** Thrown when a patient id does not match any record. Mapped to a 404 ProblemDetail. */
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
