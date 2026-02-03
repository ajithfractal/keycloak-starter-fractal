package com.fractalhive.keycloak.autoconfigure;

import com.fractalhive.keycloak.config.SecurityConfig;
import com.fractalhive.keycloak.config.WebClientConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for Keycloak authentication and authorization.
 * This class is automatically discovered by Spring Boot when the starter is included.
 */
@AutoConfiguration
@ConditionalOnClass({EnableWebSecurity.class, WebClient.class})
@ConditionalOnProperty(prefix = "fractalhive.keycloak", name = "server-url")
@EnableConfigurationProperties(KeycloakAuthProperties.class)
@Import({SecurityConfig.class, WebClientConfig.class})
public class KeycloakAuthAutoConfiguration {

    private final KeycloakAuthProperties properties;

    public KeycloakAuthAutoConfiguration(KeycloakAuthProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates required Keycloak configuration properties at startup.
     * Throws IllegalArgumentException with clear error messages if required properties are missing.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!StringUtils.hasText(properties.getServerUrl())) {
            throw new IllegalArgumentException(
                    "Keycloak configuration is missing required property: 'fractalhive.keycloak.server-url'. " +
                    "Please add this property to your application.properties or application.yml"
            );
        }

        if (!StringUtils.hasText(properties.getRealm())) {
            throw new IllegalArgumentException(
                    "Keycloak configuration is missing required property: 'fractalhive.keycloak.realm'. " +
                    "Please add this property to your application.properties or application.yml"
            );
        }

        if (!StringUtils.hasText(properties.getClientId())) {
            throw new IllegalArgumentException(
                    "Keycloak configuration is missing required property: 'fractalhive.keycloak.client-id'. " +
                    "This MUST be unique per application. Please add this property to your application.properties or application.yml"
            );
        }

        if (!StringUtils.hasText(properties.getClientSecret())) {
            throw new IllegalArgumentException(
                    "Keycloak configuration is missing required property: 'fractalhive.keycloak.client-secret'. " +
                    "This MUST be unique per application. Please add this property to your application.properties or application.yml"
            );
        }
    }

    /**
     * WebClient bean for Keycloak API calls.
     * Can be overridden by consuming applications if needed.
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient keycloakWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
