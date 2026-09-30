package com.mediscreen.assessment.dto;

import java.util.List;

/** One page of a patient's notes, as note-service returns it. */
public record NotePage(List<Note> content, int totalPages) {}
