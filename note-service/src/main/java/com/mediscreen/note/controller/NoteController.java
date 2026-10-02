package com.mediscreen.note.controller;

import com.mediscreen.note.dto.NoteRequest;
import com.mediscreen.note.dto.NoteResponse;
import com.mediscreen.note.dto.PageResponse;
import com.mediscreen.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes", description = "Practitioners' notes on a patient's history")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List a patient's notes, newest first, one page at a time")
    @ApiResponse(responseCode = "200", description = "The patient's notes, possibly none")
    @ApiResponse(responseCode = "400", description = "patientId is missing or invalid")
    public PageResponse<NoteResponse> list(
            @RequestParam long patientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.findByPatientId(patientId, pageable));
    }

    @PostMapping
    @Operation(summary = "Add a note to a patient's history")
    @ApiResponse(responseCode = "201", description = "The note was added")
    @ApiResponse(responseCode = "400", description = "The submitted note is invalid")
    public ResponseEntity<NoteResponse> add(@Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(request));
    }
}
