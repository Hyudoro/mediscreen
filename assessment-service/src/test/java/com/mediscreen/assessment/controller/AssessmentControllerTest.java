package com.mediscreen.assessment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.mediscreen.assessment.config.SecurityConfig;
import com.mediscreen.assessment.dto.AssessmentResponse;
import com.mediscreen.assessment.exception.PatientNotFoundException;
import com.mediscreen.assessment.service.AssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.ResourceAccessException;

@WebMvcTest(AssessmentController.class)
@Import(SecurityConfig.class)
@DisplayName("The assessment endpoint")
class AssessmentControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AssessmentService service;

    @Nested
    @DisplayName("without credentials")
    class Unauthenticated {

        @Test
        @DisplayName("answers 401 rather than assessing the patient")
        void refusesToAssess() {
            assertThat(mvc.get().uri("/api/v1/assessments/3")).hasStatus(HttpStatus.UNAUTHORIZED);
            then(service).should(never()).assess(anyLong());
        }
    }

    @Nested
    @DisplayName("with credentials")
    class Authenticated {

        @Test
        @DisplayName("answers the patient id and the risk level, nothing else")
        void answersTheRiskLevel() {
            given(service.assess(3L)).willReturn(new AssessmentResponse(3L, "InDanger"));

            var result = mvc.perform(get("/api/v1/assessments/3").with(user("tester")));

            assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
            assertThat(result).bodyJson().isStrictlyEqualTo("""
                    {"patientId":3,"riskLevel":"InDanger"}
                    """);
        }

        @Test
        @DisplayName("answers 404 as a ProblemDetail for an unknown patient")
        void answers404ForAnUnknownPatient() {
            given(service.assess(99L)).willThrow(new PatientNotFoundException(99L));

            var result = mvc.perform(get("/api/v1/assessments/99").with(user("tester")));

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Patient not found");
            assertThat(result).bodyJson().extractingPath("$.patientId").isEqualTo(99);
        }

        @Test
        @DisplayName("answers 502 as a ProblemDetail when the patient's data cannot be read")
        void answers502WhenTheDataCannotBeRead() {
            given(service.assess(3L)).willThrow(new ResourceAccessException("Connection refused"));

            var result = mvc.perform(get("/api/v1/assessments/3").with(user("tester")));

            assertThat(result).hasStatus(HttpStatus.BAD_GATEWAY).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Patient data unavailable");
        }

        @Test
        @DisplayName("answers 400 as a ProblemDetail when the id is not a number")
        void answers400ForAnIdThatIsNotANumber() {
            assertThat(mvc.perform(get("/api/v1/assessments/abc").with(user("tester"))))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            then(service).should(never()).assess(anyLong());
        }
    }
}
