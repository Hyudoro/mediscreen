package com.mediscreen.frontend.config;

import com.mediscreen.frontend.client.PatientClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Builds the HTTP client the front uses to reach the gateway.
 *
 * <p>The browser session and the service credentials are two different things: the user logs in
 * with a form, and the front then presents its own Basic credentials to the gateway server-side.
 * The user's password is never forwarded.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
@ImportHttpServices(group = "gateway", types = PatientClient.class)
public class GatewayClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer gatewayClientConfigurer(GatewayProperties properties) {
        var credentials = Base64.getEncoder()
                .encodeToString((properties.username() + ":" + properties.password()).getBytes(StandardCharsets.UTF_8));

        return groups -> groups.filterByName("gateway")
                .forEachClient((group, builder) -> builder.baseUrl(properties.uri())
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials));
    }
}
