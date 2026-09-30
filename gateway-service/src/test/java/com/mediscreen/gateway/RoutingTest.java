package com.mediscreen.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The gateway in front of three stand-in services. Each answers with its own name, the path it
 * received and the Authorization header it was given, or the Host header when asked for its API
 * description.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The gateway's routes")
class RoutingTest {

    private static final HttpServer PATIENT_SERVICE = standIn("patient-service");
    private static final HttpServer NOTE_SERVICE = standIn("note-service");
    private static final HttpServer ASSESSMENT_SERVICE = standIn("assessment-service");

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("mediscreen.services.patient-uri", () -> uri(PATIENT_SERVICE));
        registry.add("mediscreen.services.note-uri", () -> uri(NOTE_SERVICE));
        registry.add("mediscreen.services.assessment-uri", () -> uri(ASSESSMENT_SERVICE));
    }

    @AfterAll
    static void stopTheServices() {
        PATIENT_SERVICE.stop(0);
        NOTE_SERVICE.stop(0);
        ASSESSMENT_SERVICE.stop(0);
    }

    @LocalServerPort
    private int port;

    @ParameterizedTest(name = "{0} goes to {1}")
    @DisplayName("send each API path to its service, with the caller's credentials")
    @CsvSource({
        "/api/v1/patients/1, patient-service",
        "/api/v1/notes?patientId=1, note-service",
        "/api/v1/assessments/1, assessment-service"
    })
    void sendEachPathToItsService(String path, String service) {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Basic dGVzdDp0ZXN0")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo(service + " " + path + " Basic dGVzdDp0ZXN0");
    }

    @ParameterizedTest(name = "{0} goes to {1}")
    @DisplayName("fetch each service's API description, with the caller's Host header")
    @CsvSource({
        "/api-docs/patient-service, patient-service",
        "/api-docs/note-service, note-service",
        "/api-docs/assessment-service, assessment-service"
    })
    void fetchEachApiDescription(String path, String service) {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo(service + " /v3/api-docs localhost:" + port);
    }

    private static HttpServer standIn(String name) {
        try {
            var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", exchange -> answer(exchange, name, HttpHeaders.AUTHORIZATION));
            server.createContext("/v3/api-docs", exchange -> answer(exchange, name, HttpHeaders.HOST));
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void answer(HttpExchange exchange, String name, String header) throws IOException {
        var body = (name + " " + exchange.getRequestURI() + " "
                        + exchange.getRequestHeaders().getFirst(header))
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String uri(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
