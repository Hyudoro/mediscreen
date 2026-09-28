package com.mediscreen.patient.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** One page of a list as the API exposes it: the items, and what a client needs to page through. */
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
