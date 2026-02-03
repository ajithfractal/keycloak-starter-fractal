package com.fractalhive.keycloak.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts JWT from cookies and adds it to Authorization header.
 * Supports both cookie and header-based authentication.
 */
@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpServletRequest newRequest = request;

        // If Authorization header already exists, don't override
        if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {

            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                        newRequest = new AuthorizationHeaderRequestWrapper(
                                request,
                                "Bearer " + cookie.getValue()
                        );
                        break;
                    }
                }
            }
        }

        filterChain.doFilter(newRequest, response);
    }
}
