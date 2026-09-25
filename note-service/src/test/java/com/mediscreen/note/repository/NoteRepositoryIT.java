package com.mediscreen.note.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediscreen.note.TestcontainersConfiguration;
import com.mediscreen.note.config.MongoConfig;
import com.mediscreen.note.domain.Note;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;

/**
 * Runs against a real MongoDB, with the service's own converter. The demo seed is not loaded: every
 * note checked here is saved by the test.
 */
@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoConfig.class})
@DisplayName("The note repository")
class NoteRepositoryIT {

    private static final Instant NINE_AM = Instant.parse("2026-09-01T09:00:00Z");

    @Autowired
    private NoteRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void emptyTheCollection() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("finding a patient's notes")
    class Finding {

        @Test
        @DisplayName("returns that patient's notes only, in the order asked for")
        void returnsThatPatientsNotesOnly() {
            repository.saveAll(List.of(
                    Note.create(1, "TestNone", "first", NINE_AM),
                    Note.create(2, "TestBorderline", "another patient", NINE_AM.plusSeconds(60)),
                    Note.create(1, "TestNone", "second", NINE_AM.plusSeconds(120))));

            var page = repository.findByPatientId(1, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

            assertThat(page.getContent()).extracting(Note::note).containsExactly("second", "first");
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns an empty page for a patient with no notes")
        void returnsAnEmptyPage() {
            assertThat(repository.findByPatientId(99, PageRequest.of(0, 20))).isEmpty();
        }
    }

    @Nested
    @DisplayName("storing a note")
    class Storing {

        @Test
        @DisplayName("writes the fields the seed script writes, and no others")
        void writesTheSeedFields() {
            repository.save(Note.create(1, "TestNone", "Poids", NINE_AM));

            var stored = mongoTemplate.getCollection("notes").find().first();

            assertThat(stored).isNotNull();
            assertThat(stored.keySet()).containsExactlyInAnyOrder("_id", "patientId", "patient", "note", "createdAt");
            assertThat(stored.get("patientId")).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("keeps line breaks, spacing and markup exactly as typed")
        void keepsTheTextAsTyped() {
            var text = "  Première ligne\nDeuxième ligne, crises d’apnée\r\n\n\tTroisième <b>ligne</b>  ";

            var saved = repository.save(Note.create(4, "TestEarlyOnset", text, NINE_AM));

            assertThat(repository.findById(saved.id()))
                    .get()
                    .extracting(Note::note)
                    .isEqualTo(text);
        }

        @Test
        @DisplayName("accepts a very long note without truncating it")
        void acceptsAVeryLongNote() {
            var text = "Poids égal ou inférieur au poids recommandé. ".repeat(25_000);

            var saved = repository.save(Note.create(4, "TestEarlyOnset", text, NINE_AM));

            assertThat(repository.findById(saved.id()))
                    .get()
                    .extracting(Note::note)
                    .isEqualTo(text);
        }
    }

    @Nested
    @DisplayName("the collection")
    class Collection {

        @Test
        @DisplayName("is called notes and is indexed on patientId")
        void isIndexedOnPatientId() {
            assertThat(mongoTemplate.getCollectionName(Note.class)).isEqualTo("notes");
            assertThat(mongoTemplate.indexOps(Note.class).getIndexInfo())
                    .anySatisfy(index -> assertThat(index.getIndexFields())
                            .extracting(IndexField::getKey)
                            .containsExactly("patientId"));
        }
    }
}
