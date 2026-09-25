package com.mediscreen.frontend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The add-a-note form, a class for the same reason as {@link PatientForm}.
 *
 * <p>The text has no {@code @Size}: the client asked for no limit on the size of a note. It is sent
 * on exactly as typed, line breaks included.
 */
public class NoteForm {

    @NotBlank
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
