package com.keves.dreamreach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;

/**
 * Explicitly tells Spring Security to block everything unless the user provides credentials.
 * This is the foundation of our "Zero Trust" architecture.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // handles constructor injection automatically
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // force CORS to allow preflight at login
                .cors(Customizer.withDefaults())
                // disable CSRF for stateless REST API
                .csrf(csrf -> csrf.disable())

                // define authorization rules
                .authorizeHttpRequests(auth -> auth
                        // allow anyone to check if the server is healthy
                        .requestMatchers("/api/system/health").permitAll()
                        // allow access to the H2 console for development
                        .requestMatchers("/h2-console/**").permitAll()
                        // allows anyone to access registration view
                        .requestMatchers("/api/auth/register").permitAll()
                        // allow anyone to verify their newly created account
                        .requestMatchers("/api/auth/verify").permitAll()
                        // Allow login window
                        .requestMatchers("/api/auth/login").permitAll()
                        // require authentication for everything else
                        .anyRequest().authenticated()
                )

                // tell Spring not to store user state in session (Stateless)
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Add JWT bouncer before the default bouncer
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // necessary to allow the h2 console to load its iframes
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * Registers the BCrypt hashing algorithm into the Spring application context.
     * Spring Security will automatically locate this Bean and use it to hash
     * new passwords during registration and verify hashes during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
