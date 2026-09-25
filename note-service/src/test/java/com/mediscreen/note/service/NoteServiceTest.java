package com.mediscreen.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mediscreen.note.domain.Note;
import com.mediscreen.note.dto.NoteRequest;
import com.mediscreen.note.repository.NoteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("The note service")
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    @InjectMocks
    private NoteService service;

    @Nested
    @DisplayName("when listing a patient's notes")
    class Listing {

        @Test
        @DisplayName("returns responses rather than documents")
        void returnsResponses() {
            var pageable = PageRequest.of(0, 20);
            var stored = new Note(
                    "n1",
                    1L,
                    "TestNone",
                    "Le patient déclare qu'il 'se sent très bien' Poids égal ou inférieur au poids recommandé",
                    Instant.parse("2026-09-01T09:00:00Z"));
            given(repository.findByPatientId(1L, pageable)).willReturn(new PageImpl<>(List.of(stored), pageable, 1));

            var page = service.findByPatientId(1L, pageable);

            assertThat(page.getContent()).singleElement().satisfies(note -> {
                assertThat(note.id()).isEqualTo("n1");
                assertThat(note.patientId()).isEqualTo(1L);
                assertThat(note.patient()).isEqualTo("TestNone");
                assertThat(note.note()).isEqualTo(stored.note());
                assertThat(note.createdAt()).isEqualTo(stored.createdAt());
            });
        }

        @Test
        @DisplayName("returns an empty page for a patient with no notes")
        void returnsAnEmptyPage() {
            var pageable = PageRequest.of(0, 20);
            given(repository.findByPatientId(99L, pageable)).willReturn(Page.empty(pageable));

            assertThat(service.findByPatientId(99L, pageable)).isEmpty();
        }
    }

    @Nested
    @DisplayName("when adding a note")
    class Adding {

        @Test
        @DisplayName("stores the text exactly as typed")
        void storesTheTextAsTyped() {
            given(repository.save(any(Note.class))).willAnswer(call -> call.getArgument(0));
            var text = "  Première ligne\nDeuxième ligne  \r\n";

            service.add(new NoteRequest(4L, "TestEarlyOnset", text));

            var saved = ArgumentCaptor.forClass(Note.class);
            then(repository).should().save(saved.capture());
            assertThat(saved.getValue()).satisfies(note -> {
                assertThat(note.id()).isNull();
                assertThat(note.patientId()).isEqualTo(4L);
                assertThat(note.patient()).isEqualTo("TestEarlyOnset");
                assertThat(note.note()).isEqualTo(text);
            });
        }

        @Test
        @DisplayName("stamps the note with the current time, to the millisecond")
        void stampsTheNote() {
            given(repository.save(any(Note.class))).willAnswer(call -> call.getArgument(0));

            var response = service.add(new NoteRequest(1L, "TestNone", "Poids"));

            assertThat(response.createdAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
            assertThat(response.createdAt().getNano() % 1_000_000).isZero();
        }
    }
}
