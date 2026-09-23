package com.mediscreen.frontend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.mediscreen.frontend.client.PatientClient;
import com.mediscreen.frontend.dto.PatientPage;
import com.mediscreen.frontend.dto.PatientView;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PatientWebController.class)
@DisplayName("The patient screens")
class PatientWebControllerTest {

    private static final PatientView TEST_NONE = new PatientView(
            1L, "Test", "TestNone", LocalDate.of(1966, 12, 31), "FEMALE", "1 Brookside St", "100-222-3333");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PatientClient client;

    @Nested
    @DisplayName("when signed in")
    class SignedIn {

        @Test
        @DisplayName("lists the patients")
        void listsThePatients() {
            given(client.list(0, 20)).willReturn(new PatientPage(List.of(TEST_NONE), 0, 20, 1, 1));

            assertThat(mvc.perform(get("/patients").with(user("tester"))))
                    .hasStatusOk()
                    .bodyText()
                    .contains("TestNone");
        }

        @Test
        @DisplayName("shows one patient, marking an absent address rather than hiding it")
        void showsOnePatient() {
            given(client.get(2L))
                    .willReturn(new PatientView(
                            2L, "Test", "TestBorderline", LocalDate.of(1945, 6, 24), "MALE", null, null));

            assertThat(mvc.perform(get("/patients/2").with(user("tester"))))
                    .hasStatusOk()
                    .bodyText()
                    .contains("TestBorderline")
                    .contains("Not recorded");
        }

        @Test
        @DisplayName("accepts a new patient with no address and no phone number")
        void acceptsOptionalFieldsMissing() {
            given(client.create(org.mockito.ArgumentMatchers.any())).willReturn(TEST_NONE);

            assertThat(mvc.perform(post("/patients")
                            .param("firstName", "Jane")
                            .param("lastName", "Doe")
                            .param("birthDate", "1990-01-01")
                            .param("gender", "FEMALE")
                            .param("street", "")
                            .param("phoneNumber", "")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatus(HttpStatus.FOUND);
        }

        @Test
        @DisplayName("redisplays the form when a required field is missing")
        void redisplaysTheForm() {
            assertThat(mvc.perform(post("/patients")
                            .param("firstName", "")
                            .param("lastName", "Doe")
                            .param("birthDate", "1990-01-01")
                            .param("gender", "FEMALE")
                            .with(user("tester"))
                            .with(csrf())))
                    .hasStatusOk()
                    .bodyText()
                    .contains("Add a patient");
        }
    }
}
