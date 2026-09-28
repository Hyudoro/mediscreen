package com.mediscreen.assessment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The single service account the gateway and the front authenticate with, the same one
 * patient-service and note-service accept.
 *
 * <p>The password arrives already BCrypt-encoded, from a mounted secret file. No plaintext password
 * and no hash is committed, and neither reaches the process environment.
 */
@ConfigurationProperties(prefix = "mediscreen.security")
public record ServiceCredentials(String username, String passwordHash) {}
