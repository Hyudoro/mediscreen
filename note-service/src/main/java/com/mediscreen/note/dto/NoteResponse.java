package com.mediscreen.note.dto;

import com.mediscreen.note.domain.Note;
import java.time.Instant;

/** A note as the API exposes it. Documents never leave the service layer. */
public record NoteResponse(String id, long patientId, String patient, String note, Instant createdAt) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.id(), note.patientId(), note.patient(), note.note(), note.createdAt());
    }
}
