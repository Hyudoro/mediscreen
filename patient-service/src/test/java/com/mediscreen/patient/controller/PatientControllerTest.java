package com.mediscreen.patient.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.mediscreen.patient.config.SecurityConfig;
import com.mediscreen.patient.domain.Gender;
import com.mediscreen.patient.dto.PatientRequest;
import com.mediscreen.patient.dto.PatientResponse;
import com.mediscreen.patient.exception.PatientNotFoundException;
import com.mediscreen.patient.service.PatientService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
@DisplayName("The patient endpoint")
class PatientControllerTest {

    private static final PatientResponse TEST_NONE = new PatientResponse(
            1L, "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "1 Brookside St", "100-222-3333");

    private static final String UPDATE = """
            {"firstName":"Test","lastName":"TestNone","birthDate":"1966-12-31","gender":"FEMALE","street":"9 New Road"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PatientService service;

    @Nested
    @DisplayName("without credentials")
    class Unauthenticated {

        @Test
        @DisplayName("answers 401 rather than serving patient data")
        void answers401() {
            assertThat(mvc.get().uri("/api/v1/patients/1")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("answers 401 rather than updating a patient")
        void refusesToUpdate() {
            assertThat(mvc.put()
                            .uri("/api/v1/patients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
            then(service).should(never()).update(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("with credentials")
    class Authenticated {

        @Test
        @DisplayName("lists the patients one page at a time, twenty by default")
        void listsOnePageAtATime() {
            given(service.findAll(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(TEST_NONE), PageRequest.of(0, 20), 1));

            assertThat(mvc.perform(get("/api/v1/patients").with(user("tester"))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.content[0].lastName")
                    .isEqualTo("TestNone");

            var pageable = ArgumentCaptor.forClass(Pageable.class);
            then(service).should().findAll(pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("returns one patient as JSON")
        void returnsOnePatient() {
            given(service.findById(1L)).willReturn(TEST_NONE);

            assertThat(mvc.perform(get("/api/v1/patients/1").with(user("tester"))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.lastName")
                    .isEqualTo("TestNone");
        }

        @Test
        @DisplayName("answers 404 as a ProblemDetail when the patient is unknown")
        void answers404AsProblemDetail() {
            willThrow(new PatientNotFoundException(99L)).given(service).findById(anyLong());

            assertThat(mvc.perform(get("/api/v1/patients/99").with(user("tester"))))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.title")
                    .isEqualTo("Patient not found");
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when the body is invalid")
        void answers400AsProblemDetail() {
            var invalid = """
                    {"firstName":"","lastName":"Doe","birthDate":"1990-01-01","gender":"FEMALE"}
                    """;

            assertThat(mvc.perform(post("/api/v1/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid)
                            .with(user("tester"))))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.title")
                    .isEqualTo("Invalid patient details");
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when the id is not a number")
        void answers400ForAnIdThatIsNotANumber() {
            assertThat(mvc.perform(get("/api/v1/patients/abc").with(user("tester"))))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson()
                    .extractingPath("$.title")
                    .isEqualTo("Bad Request");
        }

        @Test
        @DisplayName("accepts a patient with no address and no phone number")
        void acceptsOptionalFieldsMissing() {
            given(service.create(any()))
                    .willReturn(new PatientResponse(
                            5L, "Jane", "Doe", LocalDate.of(1990, 1, 1), Gender.FEMALE, null, null));

            var body = """
                    {"firstName":"Jane","lastName":"Doe","birthDate":"1990-01-01","gender":"FEMALE"}
                    """;

            assertThat(mvc.perform(post("/api/v1/patients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(user("tester"))))
                    .hasStatus(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("updates a patient and returns the new details")
        void updatesAPatient() {
            given(service.update(eq(1L), any()))
                    .willReturn(new PatientResponse(
                            1L, "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "9 New Road", null));

            assertThat(mvc.perform(put("/api/v1/patients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE)
                            .with(user("tester"))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.street")
                    .isEqualTo("9 New Road");

            then(service)
                    .should()
                    .update(
                            1L,
                            new PatientRequest(
                                    "Test", "TestNone", LocalDate.of(1966, 12, 31), Gender.FEMALE, "9 New Road", null));
        }

        @Test
        @DisplayName("answers 404 as a ProblemDetail when updating an unknown patient")
        void answers404WhenUpdatingAnUnknownPatient() {
            willThrow(new PatientNotFoundException(99L)).given(service).update(eq(99L), any());

            assertThat(mvc.perform(put("/api/v1/patients/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE)
                            .with(user("tester"))))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.title")
                    .isEqualTo("Patient not found");
        }
    }
}
