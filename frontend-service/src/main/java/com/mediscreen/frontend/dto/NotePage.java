package com.mediscreen.frontend.dto;

import java.util.List;

/** One page of a patient's notes, newest first, as note-service answers it. */
public record NotePage(List<NoteView> content, int number, int size, int totalPages, long totalElements) {

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return number + 1 < totalPages;
    }
}
