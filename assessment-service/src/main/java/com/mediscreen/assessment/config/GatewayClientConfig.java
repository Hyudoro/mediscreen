package com.mediscreen.assessment.config;

import com.mediscreen.assessment.client.NoteClient;
import com.mediscreen.assessment.client.PatientClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/** Builds the HTTP client that reads patients and notes through the gateway, with the service account. */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
@ImportHttpServices(
        group = "gateway",
        types = {PatientClient.class, NoteClient.class})
public class GatewayClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer gatewayClientConfigurer(GatewayProperties properties) {
        return groups -> groups.filterByName("gateway")
                .forEachClient((group, builder) -> builder.baseUrl(properties.uri())
                        .defaultHeaders(headers -> headers.setBasicAuth(properties.username(), properties.password())));
    }
}
