package com.mediscreen.gateway.config;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Stamps every request with a correlation id so one call can be followed across the services it
 * fans out to. An id supplied by the caller is kept; otherwise one is generated here.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        var correlationId =
                incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;

        var mutated = exchange.mutate()
                .request(request -> request.header(HEADER, correlationId))
                .build();
        mutated.getResponse().getHeaders().set(HEADER, correlationId);

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
