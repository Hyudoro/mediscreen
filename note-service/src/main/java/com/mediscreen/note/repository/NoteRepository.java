package com.mediscreen.note.repository;

import com.mediscreen.note.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NoteRepository extends MongoRepository<Note, String> {

    Page<Note> findByPatientId(long patientId, Pageable pageable);
}
