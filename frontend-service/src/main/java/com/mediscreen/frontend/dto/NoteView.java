package com.mediscreen.frontend.dto;

import java.time.Instant;

/** A note as the gateway returns it. */
public record NoteView(String id, long patientId, String patient, String note, Instant createdAt) {}
