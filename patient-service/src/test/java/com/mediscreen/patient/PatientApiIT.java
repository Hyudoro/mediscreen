package com.mediscreen.patient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediscreen.patient.domain.Gender;
import com.mediscreen.patient.dto.PatientRequest;
import com.mediscreen.patient.dto.PatientResponse;
import com.mediscreen.patient.repository.PatientRepository;
import java.time.LocalDate;
import java.time.Month;
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
 * PostgreSQL. The service account gets a BCrypt hash computed here, so no hash is written down.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@DisplayName("The patient API over HTTP")
class PatientApiIT {

    private static final String USERNAME = "test-service";
    private static final String PASSWORD = "integration-test-password";

    @DynamicPropertySource
    static void serviceAccount(DynamicPropertyRegistry registry) {
        registry.add("mediscreen.security.password-hash", () -> new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PatientRepository repository;

    private RestClient patients;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        patients = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/patients")
                .defaultHeaders(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .build();
    }

    @Test
    @DisplayName("refuses a request without credentials")
    void refusesAnonymousRequests() {
        var status = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/api/v1/patients")
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("adds a patient, then serves and updates it")
    void addsServesAndUpdatesAPatient() {
        var created = patients.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PatientRequest(
                        "Test",
                        "TestNone",
                        LocalDate.of(1966, Month.DECEMBER, 31),
                        Gender.FEMALE,
                        "1 Brookside St",
                        null))
                .retrieve()
                .toEntity(PatientResponse.class);
        var id = created.getBody().id();

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasPath("/api/v1/patients/" + id);
        assertThat(get(id).street()).isEqualTo("1 Brookside St");

        patients.put()
                .uri("/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PatientRequest(
                        "Test",
                        "TestNone",
                        LocalDate.of(1966, Month.DECEMBER, 31),
                        Gender.FEMALE,
                        null,
                        "100-222-3333"))
                .retrieve()
                .toBodilessEntity();

        assertThat(get(id)).satisfies(patient -> {
            assertThat(patient.street()).isNull();
            assertThat(patient.phoneNumber()).isEqualTo("100-222-3333");
        });
    }

    @Test
    @DisplayName("answers 404 as a ProblemDetail for an unknown patient")
    void answers404ForAnUnknownPatient() {
        var problem = patients.get().uri("/{id}", 999_999).exchange((request, response) -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
            return response.bodyTo(Problem.class);
        });

        assertThat(problem.title()).isEqualTo("Patient not found");
    }

    @Test
    @DisplayName("lists the patients one page at a time")
    void listsOnePageAtATime() {
        for (var lastName : List.of("TestNone", "TestBorderline", "TestInDanger")) {
            patients.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PatientRequest(
                            "Test", lastName, LocalDate.of(1990, Month.JANUARY, 1), Gender.MALE, null, null))
                    .retrieve()
                    .toBodilessEntity();
        }

        var page = patients.get()
                .uri(builder ->
                        builder.queryParam("page", 0).queryParam("size", 2).build())
                .retrieve()
                .body(PatientsPage.class);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    private PatientResponse get(long id) {
        return patients.get().uri("/{id}", id).retrieve().body(PatientResponse.class);
    }

    private record PatientsPage(List<PatientResponse> content, long totalElements) {}

    private record Problem(String title) {}
}
