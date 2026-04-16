package com.keves.dreamreach.config;

import com.keves.dreamreach.util.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * The "Checkpoint" filter. It intercepts every request to check for a valid JWT badge.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. If the header is missing or doesn't start with 'Bearer ', move to the next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the token (everything after 'Bearer ')
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractEmail(jwt);

        // 3. If we have an email and the user isn't already authenticated...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Validate the token
            if (jwtService.isTokenValid(jwt, userEmail)) {

                // 5. Create an authentication "badge" for Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        Collections.emptyList() // We aren't using Roles/Authorities yet
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Update the Security Context. The user is now officially "logged in" for this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 7. Always hand off to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}