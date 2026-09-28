package com.mediscreen.frontend.dto;

import java.util.List;

/** One page of patients, as patient-service answers it. */
public record PatientPage(List<PatientView> content, int number, int size, int totalPages, long totalElements) {

    public boolean hasPrevious() {
        return number > 0;
    }

    public boolean hasNext() {
        return number + 1 < totalPages;
    }
}
