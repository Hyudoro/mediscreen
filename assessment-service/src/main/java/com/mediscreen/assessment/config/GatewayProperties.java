package com.mediscreen.assessment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the gateway is and which service account to reach it with.
 *
 * <p>assessment-service reads patients and notes through the gateway, never from patient-service or
 * note-service directly.
 */
@ConfigurationProperties(prefix = "mediscreen.gateway")
public record GatewayProperties(String uri, String username, String password) {}
