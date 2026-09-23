package com.mediscreen.patient.dto;

import com.mediscreen.patient.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Incoming patient details, used for both creation and update.
 *
 * <p>{@code street} and {@code phoneNumber} carry no {@code @NotBlank}: the user story makes the
 * postal address and the phone number optional for a valid record.
 */
public record PatientRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotNull @Past LocalDate birthDate,
        @NotNull Gender gender,
        @Size(max = 120) String street,
        @Size(max = 20) String phoneNumber) {}
