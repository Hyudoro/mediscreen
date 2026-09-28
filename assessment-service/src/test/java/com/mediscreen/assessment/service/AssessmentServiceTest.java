package com.mediscreen.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mediscreen.assessment.client.NoteClient;
import com.mediscreen.assessment.client.PatientClient;
import com.mediscreen.assessment.domain.Gender;
import com.mediscreen.assessment.dto.AssessmentResponse;
import com.mediscreen.assessment.dto.Note;
import com.mediscreen.assessment.dto.NotePage;
import com.mediscreen.assessment.dto.Patient;
import com.mediscreen.assessment.exception.PatientNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("The assessment service")
class AssessmentServiceTest {

    private static final Clock SEPTEMBER_2026 = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC);

    /** A man born in 2004, who is 22 in September 2026 and turns 30 on 18 June 2034. */
    private static final Patient TEST_IN_DANGER = new Patient(3L, LocalDate.of(2004, 6, 18), Gender.MALE);

    @Mock
    private PatientClient patientClient;

    @Mock
    private NoteClient noteClient;

    private AssessmentService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(patientClient, noteClient, SEPTEMBER_2026);
    }

    private static NotePage page(int totalPages, String... notes) {
        return new NotePage(Arrays.stream(notes).map(Note::new).toList(), totalPages);
    }

    @Nested
    @DisplayName("for a known patient")
    class KnownPatient {

        @Test
        @DisplayName("assesses the patient from their age, gender and notes")
        void assessesThePatient() {
            given(patientClient.get(3L)).willReturn(TEST_IN_DANGER);
            given(noteClient.list(eq(3L), eq(0), anyInt()))
                    .willReturn(page(1, "Le patient est fumeur", "Crises anormales, cholestérol élevé"));

            assertThat(service.assess(3L)).isEqualTo(new AssessmentResponse(3L, "InDanger"));
        }

        @Test
        @DisplayName("reads every page of notes, not only the first")
        void readsEveryPage() {
            given(patientClient.get(3L)).willReturn(TEST_IN_DANGER);
            given(noteClient.list(eq(3L), eq(0), anyInt())).willReturn(page(2, "Le patient est fumeur", "anormal"));
            given(noteClient.list(eq(3L), eq(1), anyInt())).willReturn(page(2, "Cholestérol élevé"));

            // Two triggers on the first page would leave this young man at None.
            assertThat(service.assess(3L).riskLevel()).isEqualTo("InDanger");
        }

        @Test
        @DisplayName("gives None to a patient without notes")
        void givesNoneWithoutNotes() {
            given(patientClient.get(3L)).willReturn(TEST_IN_DANGER);
            given(noteClient.list(eq(3L), eq(0), anyInt())).willReturn(page(0));

            assertThat(service.assess(3L).riskLevel()).isEqualTo("None");
        }

        @Test
        @DisplayName("takes the patient's age on the day of the assessment")
        void takesTheAgeOnTheDay() {
            var service = new AssessmentService(
                    patientClient, noteClient, Clock.fixed(Instant.parse("2034-06-18T10:00:00Z"), ZoneOffset.UTC));
            given(patientClient.get(3L)).willReturn(TEST_IN_DANGER);
            given(noteClient.list(eq(3L), eq(0), anyInt()))
                    .willReturn(page(1, "Le patient est fumeur", "Crises anormales, cholestérol élevé"));

            // The same three triggers, on his 30th birthday.
            assertThat(service.assess(3L).riskLevel()).isEqualTo("Borderline");
        }
    }

    @Nested
    @DisplayName("for an unknown patient")
    class UnknownPatient {

        @Test
        @DisplayName("throws PatientNotFoundException without reading any notes")
        void throwsPatientNotFound() {
            given(patientClient.get(99L))
                    .willThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), new byte[0], null));

            assertThatThrownBy(() -> service.assess(99L))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining("99");
            then(noteClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("when a service cannot answer")
    class Unavailable {

        @Test
        @DisplayName("lets the error through when the gateway cannot be reached")
        void gatewayDown() {
            given(patientClient.get(3L)).willThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> service.assess(3L)).isInstanceOf(ResourceAccessException.class);
        }

        @Test
        @DisplayName("lets the error through when note-service is down")
        void noteServiceDown() {
            given(patientClient.get(3L)).willReturn(TEST_IN_DANGER);
            given(noteClient.list(eq(3L), eq(0), anyInt()))
                    .willThrow(HttpServerErrorException.create(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Service Unavailable",
                            new HttpHeaders(),
                            new byte[0],
                            null));

            assertThatThrownBy(() -> service.assess(3L)).isInstanceOf(HttpServerErrorException.class);
        }
    }
}
