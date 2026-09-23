package com.mediscreen.patient.dto;

import com.mediscreen.patient.domain.Gender;
import com.mediscreen.patient.domain.Patient;
import java.time.LocalDate;

/** A patient as the API exposes it. Entities never leave the service layer. */
public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Gender gender,
        String street,
        String phoneNumber) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getStreet().orElse(null),
                patient.getPhoneNumber());
    }
}
