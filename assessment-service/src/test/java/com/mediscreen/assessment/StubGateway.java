package com.mediscreen.assessment;

import com.mediscreen.assessment.ClientTestData.TestPatient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stands in for the gateway: serves the client's test patients and their notes in the JSON
 * patient-service and note-service write, one page of notes at a time, and answers 401 to a request
 * without the service account's credentials.
 */
final class StubGateway implements AutoCloseable {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpServer server;
    private final String credentials;

    private StubGateway(HttpServer server, String credentials) {
        this.server = server;
        this.credentials = credentials;
    }

    static StubGateway start(String username, String password) {
        try {
            var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            var gateway = new StubGateway(
                    server, "Basic " + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8));
            server.createContext("/api/v1/patients/", gateway::patient);
            server.createContext("/api/v1/notes", gateway::notes);
            server.start();
            return gateway;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void patient(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            return;
        }
        var id = Long.parseLong(exchange.getRequestURI().getPath().substring("/api/v1/patients/".length()));
        var patient = find(id);
        if (patient.isEmpty()) {
            send(exchange, 404, "application/problem+json", new Problem("Patient not found", 404));
            return;
        }
        send(exchange, 200, "application/json", PatientBody.from(patient.get()));
    }

    private void notes(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            return;
        }
        var query = Arrays.stream(exchange.getRequestURI().getQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
        var patientId = Long.parseLong(query.get("patientId"));
        var page = Integer.parseInt(query.get("page"));
        var size = Integer.parseInt(query.get("size"));

        var notes = find(patientId).map(TestPatient::notes).orElse(List.of());
        var content = IntStream.range(0, notes.size())
                .skip((long) page * size)
                .limit(size)
                .mapToObj(index -> new NoteBody("n" + patientId + "-" + index, patientId, notes.get(index)))
                .toList();
        var totalPages = (notes.size() + size - 1) / size;
        send(exchange, 200, "application/json", new PageBody(content, page, size, notes.size(), totalPages));
    }

    private boolean authorized(HttpExchange exchange) throws IOException {
        if (credentials.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            return true;
        }
        exchange.sendResponseHeaders(401, -1);
        exchange.close();
        return false;
    }

    private static Optional<TestPatient> find(long id) {
        return ClientTestData.PATIENTS.stream()
                .filter(patient -> patient.id() == id)
                .findFirst();
    }

    private static void send(HttpExchange exchange, int status, String contentType, Object body) throws IOException {
        var bytes = JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** The fields of patient-service's PatientResponse. */
    private record PatientBody(
            long id,
            String firstName,
            String lastName,
            String birthDate,
            String gender,
            String street,
            String phoneNumber) {

        static PatientBody from(TestPatient patient) {
            return new PatientBody(
                    patient.id(),
                    "Test",
                    patient.lastName(),
                    patient.birthDate().toString(),
                    patient.gender().name(),
                    patient.street(),
                    patient.phoneNumber());
        }
    }

    /** The fields of note-service's NoteResponse. */
    private record NoteBody(String id, long patientId, String patient, String note, String createdAt) {

        NoteBody(String id, long patientId, String note) {
            this(id, patientId, find(patientId).orElseThrow().lastName(), note, Instant.EPOCH.toString());
        }
    }

    /** The fields of note-service's PageResponse. */
    private record PageBody(List<NoteBody> content, int number, int size, long totalElements, int totalPages) {}

    private record Problem(String title, int status) {}
}
