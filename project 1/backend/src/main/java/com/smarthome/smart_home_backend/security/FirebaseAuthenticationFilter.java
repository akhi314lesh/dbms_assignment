package com.smarthome.smart_home_backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.service.UserProvisioningService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);

    private final FirebaseAuth firebaseAuth;
    private final UserProvisioningService userProvisioningService;

    public FirebaseAuthenticationFilter(
            FirebaseAuth firebaseAuth,
            UserProvisioningService userProvisioningService) {
        this.firebaseAuth = firebaseAuth;
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No Bearer token provided: proceed along chain.
            // Spring Security will reject if the requested endpoint requires authentication.
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7).trim();
        if (idToken.isEmpty()) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Bearer token is empty.");
            return;
        }

        FirebaseToken decodedToken;
        try {
            decodedToken = firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase ID token verification failed: {}", e.getMessage());
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Invalid or expired Firebase ID token.");
            return;
        } catch (Exception e) {
            log.error("Unexpected error during Firebase token verification: {}", e.getClass().getSimpleName());
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Token verification failed.");
            return;
        }

        User oracleUser;
        try {
            oracleUser = userProvisioningService.resolveOrProvisionUser(decodedToken);
        } catch (Exception e) {
            log.error("Failed to resolve or provision Oracle user for Firebase UID: {}", decodedToken.getUid(), e);
            sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "Failed to resolve application user account.");
            return;
        }

        FirebaseUserDetails principal = new FirebaseUserDetails(decodedToken.getUid(), decodedToken.getEmail(), oracleUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void sendJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status, error, message
        ));
    }
}
