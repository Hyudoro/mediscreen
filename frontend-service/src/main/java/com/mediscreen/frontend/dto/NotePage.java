package com.mediscreen.frontend.dto;

import java.util.List;

/** One page of a patient's notes, newest first, mirroring the fields Spring Data serialises. */
public record NotePage(List<NoteView> content, int number, int size, int totalPages, long totalElements) {

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return number + 1 < totalPages;
    }
}
