package com.mediscreen.gateway.config;

import static com.mediscreen.gateway.config.CorrelationIdFilter.HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("The correlation id filter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    @Test
    @DisplayName("keeps the id the caller sent, on the way in and on the way out")
    void keepsTheCallersId() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/patients").header(HEADER, "check-123"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HEADER)).isEqualTo("check-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER)).isEqualTo("check-123");
    }

    @Test
    @DisplayName("generates an id when the caller sent none")
    void generatesAnIdWhenMissing() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/patients"));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        var id = forwarded.get().getRequest().getHeaders().getFirst(HEADER);
        assertThat(UUID.fromString(id)).hasToString(id);
        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER)).isEqualTo(id);
    }

    @Test
    @DisplayName("generates an id when the caller sent a blank one")
    void generatesAnIdWhenBlank() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/patients").header(HEADER, "  "));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        var id = forwarded.get().getRequest().getHeaders().getFirst(HEADER);
        assertThat(UUID.fromString(id)).hasToString(id);
        assertThat(exchange.getResponse().getHeaders().getFirst(HEADER)).isEqualTo(id);
    }
}
