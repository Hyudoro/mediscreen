package com.mediscreen.note.service;

import com.mediscreen.note.domain.Note;
import com.mediscreen.note.dto.NoteRequest;
import com.mediscreen.note.dto.NoteResponse;
import com.mediscreen.note.repository.NoteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Note use cases: a patient's history, and a new note for the current visit. */
@Service
public class NoteService {

    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public Page<NoteResponse> findByPatientId(long patientId, Pageable pageable) {
        return repository.findByPatientId(patientId, pageable).map(NoteResponse::from);
    }

    public NoteResponse add(NoteRequest request) {
        // MongoDB stores dates to the millisecond, so the response carries what a later read returns.
        var createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var note = Note.create(request.patientId(), request.patient(), request.note(), createdAt);
        return NoteResponse.from(repository.save(note));
    }
}
