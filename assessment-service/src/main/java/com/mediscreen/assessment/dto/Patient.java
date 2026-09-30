package com.mediscreen.assessment.dto;

import com.mediscreen.assessment.domain.Gender;
import java.time.LocalDate;

/** A patient as patient-service returns it, reduced to what the risk rules need. */
public record Patient(long id, LocalDate birthDate, Gender gender) {}
