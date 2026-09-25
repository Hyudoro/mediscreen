package com.mediscreen.note;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediscreen.note.domain.Note;
import com.mediscreen.note.dto.NoteRequest;
import com.mediscreen.note.dto.NoteResponse;
import com.mediscreen.note.repository.NoteRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * The whole service over real HTTP: Basic authentication, JSON in both directions and a real
 * MongoDB. The service account gets a BCrypt hash computed here, so no hash is written down.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@DisplayName("The note API over HTTP")
class NoteApiIT {

    private static final String USERNAME = "test-service";
    private static final String PASSWORD = "integration-test-password";

    @DynamicPropertySource
    static void serviceAccount(DynamicPropertyRegistry registry) {
        registry.add("mediscreen.security.password-hash", () -> new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private NoteRepository repository;

    private RestClient notes;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        notes = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/notes")
                .defaultHeaders(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .build();
    }

    @Test
    @DisplayName("refuses a request without credentials")
    void refusesAnonymousRequests() {
        var status = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/api/v1/notes?patientId=1")
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("gives a long multi-line note back exactly as it was sent")
    void keepsTheNoteIntact() {
        var text = "Première ligne\nDeuxième ligne, avec des espaces   \r\n\n" + "Poids anormal. ".repeat(50_000);

        var created = notes.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new NoteRequest(42L, "TestNone", text))
                .retrieve()
                .toEntity(NoteResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(listFor(42L).content()).singleElement().satisfies(note -> {
            assertThat(note.id()).isEqualTo(created.getBody().id());
            assertThat(note.note()).isEqualTo(text);
        });
    }

    @Test
    @DisplayName("lists a patient's notes newest first")
    void listsNewestFirst() {
        var nineAm = Instant.parse("2026-09-01T09:00:00Z");
        repository.saveAll(List.of(
                Note.create(7, "TestInDanger", "older", nineAm),
                Note.create(7, "TestInDanger", "newer", nineAm.plusSeconds(3600)),
                Note.create(8, "TestEarlyOnset", "another patient", nineAm.plusSeconds(7200))));

        assertThat(listFor(7L).content()).extracting(NoteResponse::note).containsExactly("newer", "older");
    }

    private NotesPage listFor(long patientId) {
        return notes.get()
                .uri(builder -> builder.queryParam("patientId", patientId).build())
                .retrieve()
                .body(NotesPage.class);
    }

    private record NotesPage(List<NoteResponse> content, long totalElements) {}
}
