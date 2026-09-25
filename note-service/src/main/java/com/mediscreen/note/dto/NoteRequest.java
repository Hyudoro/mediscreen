package com.mediscreen.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A note to add to a patient's history.
 *
 * <p>{@code note} carries no {@code @Size}: the client asked for no limit on the size of a note.
 * {@code patient} is a copy of the patient's last name, so it has the same limit as in
 * patient-service.
 */
public record NoteRequest(
        @NotNull @Positive Long patientId,
        @NotBlank @Size(max = 60) String patient,
        @NotBlank String note) {}
