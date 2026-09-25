package com.mediscreen.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the gateway is and which service account to reach it with.
 *
 * <p>The front never calls a back service directly: every outgoing call goes through the gateway.
 */
@ConfigurationProperties(prefix = "mediscreen.gateway")
public record GatewayProperties(String uri, String username, String password) {}
