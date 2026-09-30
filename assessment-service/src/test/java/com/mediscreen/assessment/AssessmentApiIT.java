package com.mediscreen.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediscreen.assessment.dto.AssessmentResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.web.client.RestClient;

/**
 * The whole service over real HTTP, in front of a stand-in gateway that serves the client's test
 * patients and notes. The clock is fixed to the day the expected levels were checked: patient 4
 * turns 30 in 2032, and their level changes then.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The assessment API over HTTP")
class AssessmentApiIT {

    private static final String USERNAME = "test-service";
    private static final String PASSWORD = "integration-test-password";

    /** Checks the account the test configuration gives assessment-service for its own calls. */
    private static final StubGateway GATEWAY = StubGateway.start("test-service", "test-password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("mediscreen.gateway.uri", GATEWAY::uri);
        registry.add("mediscreen.security.password-hash", () -> new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);
    }

    @AfterAll
    static void stopTheGateway() {
        GATEWAY.close();
    }

    @LocalServerPort
    private int port;

    private RestClient assessments;

    @BeforeEach
    void setUp() {
        assessments = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/assessments")
                .defaultHeaders(headers -> headers.setBasicAuth(USERNAME, PASSWORD))
                .build();
    }

    @ParameterizedTest(name = "patient {0}: {1}")
    @DisplayName("gives each of the client's test patients the level the sprint 3 sheet expects")
    @CsvSource({"1, None", "2, Borderline", "3, InDanger", "4, EarlyOnset"})
    void givesTheExpectedLevel(long patientId, String expected) {
        var assessment = assessments.get().uri("/{id}", patientId).retrieve().body(AssessmentResponse.class);

        assertThat(assessment).isEqualTo(new AssessmentResponse(patientId, expected));
    }

    @Test
    @DisplayName("refuses a request without credentials")
    void refusesAnonymousRequests() {
        var status = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/api/v1/assessments/1")
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("answers 404 as a ProblemDetail for an unknown patient")
    void answers404ForAnUnknownPatient() {
        var problem = assessments.get().uri("/{id}", 99).exchange((request, response) -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
            return response.bodyTo(Problem.class);
        });

        assertThat(problem.title()).isEqualTo("Patient not found");
    }

    private record Problem(String title) {}
}
