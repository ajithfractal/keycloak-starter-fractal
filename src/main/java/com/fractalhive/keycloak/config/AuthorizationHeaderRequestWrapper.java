package com.fractalhive.keycloak.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.HttpHeaders;

/**
 * Request wrapper that adds Authorization header from cookie.
 */
public class AuthorizationHeaderRequestWrapper extends HttpServletRequestWrapper {

    private final String authorizationHeader;

    public AuthorizationHeaderRequestWrapper(
            HttpServletRequest request,
            String authorizationHeader
    ) {
        super(request);
        this.authorizationHeader = authorizationHeader;
    }

    @Override
    public String getHeader(String name) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
            return authorizationHeader;
        }
        return super.getHeader(name);
    }
}
