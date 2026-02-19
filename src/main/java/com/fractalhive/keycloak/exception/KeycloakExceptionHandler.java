package com.fractalhive.keycloak.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Keycloak authentication exceptions.
 * Handles exceptions thrown by filters and controllers.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class KeycloakExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * Handles missing token exceptions.
     */
    @ExceptionHandler(MissingTokenException.class)
    public ResponseEntity<Map<String, Object>> handleMissingToken(MissingTokenException ex) {
        log.warn("Missing token: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Handles invalid token exceptions.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Handles expired token exceptions.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredToken(TokenExpiredException ex) {
        log.warn("Expired token: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles general Keycloak authentication exceptions.
     */
    @ExceptionHandler(KeycloakAuthException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloakAuthException(KeycloakAuthException ex) {
        log.error("Keycloak authentication error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Handles Spring Security JWT exceptions.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        log.warn("JWT validation error: {}", ex.getMessage());
        
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        
        if (message != null && (message.contains("expired") || message.contains("Expired"))) {
            status = HttpStatus.FORBIDDEN;
            message = "Token expired";
        } else {
            message = "Invalid token: " + (message != null ? message : "Token validation failed");
        }
        
        return buildErrorResponse(status, message);
    }

    /**
     * Handles filter exceptions by writing directly to response.
     * This is used when exceptions are thrown from filters before controllers.
     */
    public void handleFilterException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Authentication error";

        if (ex instanceof MissingTokenException) {
            status = HttpStatus.UNAUTHORIZED;
            message = ex.getMessage();
            log.warn("Missing token: {}", message);
        } else if (ex instanceof InvalidTokenException) {
            status = HttpStatus.UNAUTHORIZED;
            message = ex.getMessage();
            log.warn("Invalid token: {}", message);
        } else if (ex instanceof TokenExpiredException) {
            status = HttpStatus.FORBIDDEN;
            message = ex.getMessage();
            log.warn("Expired token: {}", message);
        } else if (ex instanceof KeycloakAuthException) {
            status = HttpStatus.UNAUTHORIZED;
            message = ex.getMessage();
            log.error("Keycloak authentication error: {}", message, ex);
        } else if (ex instanceof org.springframework.security.oauth2.jwt.JwtException) {
            String jwtMessage = ex.getMessage();
            if (jwtMessage != null && (jwtMessage.contains("expired") || jwtMessage.contains("Expired"))) {
                status = HttpStatus.FORBIDDEN;
                message = "Token expired";
            } else {
                status = HttpStatus.UNAUTHORIZED;
                message = "Invalid token: " + (jwtMessage != null ? jwtMessage : "Token validation failed");
            }
            log.warn("JWT validation error: {}", jwtMessage);
        } else {
            log.error("Unexpected authentication error", ex);
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("path", request.getRequestURI());
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    /**
     * Builds error response entity.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", status.value());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
