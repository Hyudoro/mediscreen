package com.mediscreen.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The account the clinic staff signs in with. The hash comes from a mounted secret file. */
@ConfigurationProperties(prefix = "mediscreen.ui")
public record UiCredentials(String username, String passwordHash) {}
