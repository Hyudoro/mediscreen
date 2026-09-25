package com.mediscreen.note.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A practitioner's note, as stored in the notes collection.
 *
 * <p>Notes are found by {@code patientId}, the id patient-service gives the patient. {@code patient}
 * is the last name as the client's note sheet carries it: a label copied when the note is written,
 * not the link. The text is kept exactly as typed and has no size limit.
 */
@Document(collection = "notes")
public record Note(@Id String id, @Indexed long patientId, String patient, String note, Instant createdAt) {

    /** A note not saved yet. MongoDB assigns the id on insert. */
    public static Note create(long patientId, String patient, String note, Instant createdAt) {
        return new Note(null, patientId, patient, note, createdAt);
    }
}
