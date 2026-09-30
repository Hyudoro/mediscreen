package com.mediscreen.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The gateway's Swagger UI")
class SwaggerUiTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("opens at /swagger-ui.html")
    void opensAtTheUsualAddress() {
        var client = client();
        var page = client.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus()
                .isFound()
                .expectBody()
                .returnResult()
                .getResponseHeaders()
                .getLocation();
        client.get()
                .uri(page.toString())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    @DisplayName("lists the three services, at the paths the gateway routes, and opens on patient-service")
    void listsTheThreeServices() {
        client().get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json("""
                        {"urls": [
                          {"name": "patient-service", "url": "/api-docs/patient-service"},
                          {"name": "note-service", "url": "/api-docs/note-service"},
                          {"name": "assessment-service", "url": "/api-docs/assessment-service"}
                        ],
                        "urls.primaryName": "patient-service"}
                        """);
    }

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }
}
