package com.mediscreen.frontend.dto;

import java.time.LocalDate;

/** A patient as the gateway returns it. */
public record PatientView(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        String street,
        String phoneNumber) {}
