package com.mediscreen.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A session-backed login form for the people using the UI.
 *
 * <p>No registration and no roles: the guide says neither is needed. Every authenticated user sees
 * the same screens.
 */
@Configuration
@EnableConfigurationProperties(UiCredentials.class)
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        requests -> requests.requestMatchers("/login", "/css/**", "/actuator/health/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/patients", true))
                .logout(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UiCredentials credentials) {
        return new InMemoryUserDetailsManager(User.withUsername(credentials.username())
                .password(credentials.passwordHash())
                .build());
    }
}
