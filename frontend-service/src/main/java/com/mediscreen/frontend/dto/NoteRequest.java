package com.mediscreen.frontend.dto;

/** The body sent to add a note: the patient's id, the last name as the note sheet carries it, and the text. */
public record NoteRequest(long patientId, String patient, String note) {}
