# Troubleshooting Guide

## Properties Not Being Loaded

If the Keycloak properties are not being loaded, check the following:

### 1. Verify Property Names

Make sure your `application.properties` uses the correct property names with hyphens:

```properties
# ✅ Correct (kebab-case)
fractalhive.keycloak.server-url=https://keycloak.example.com
fractalhive.keycloak.client-id=your-client-id
fractalhive.keycloak.client-secret=your-client-secret

# ❌ Wrong (camelCase won't work)
fractalhive.keycloak.serverUrl=https://keycloak.example.com
fractalhive.keycloak.clientId=your-client-id
```

### 2. Check File Location

Ensure `application.properties` is in the correct location:
```
src/main/resources/application.properties
```

### 3. Verify Auto-Configuration is Loading

Check your application startup logs for:
- `KeycloakAuthAutoConfiguration` being loaded
- Any validation errors about missing properties

If you see validation errors, the properties are being read but are empty/null.

### 4. Enable Debug Logging

Add to your `application.properties` to see what's being loaded:

```properties
# Enable Spring Boot configuration debug
debug=true

# Or enable only configuration properties logging
logging.level.org.springframework.boot.context.properties=DEBUG
```

### 5. Check for Property Conflicts

Make sure you don't have multiple `application.properties` files or profile-specific files that might override your settings:
- `application.properties`
- `application-dev.properties`
- `application-prod.properties`
- `application.yml`

### 6. Verify Dependency is Included

Ensure the starter is in your `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 7. Check Auto-Configuration Import

Verify the auto-configuration file exists in the starter:
```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Should contain:
```
com.fractalhive.keycloak.autoconfigure.KeycloakAuthAutoConfiguration
```

### 8. Test Property Binding

Add a simple test to verify properties are loaded:

```java
@Autowired
private KeycloakAuthProperties properties;

@PostConstruct
public void testProperties() {
    System.out.println("Server URL: " + properties.getServerUrl());
    System.out.println("Client ID: " + properties.getClientId());
}
```

### 9. Common Issues

**Issue**: Properties are null
- **Solution**: Check property names match exactly (case-sensitive, use hyphens)

**Issue**: Auto-configuration not loading
- **Solution**: Ensure `fractalhive.keycloak.server-url` is present (required for `@ConditionalOnProperty`)

**Issue**: Validation errors at startup
- **Solution**: This is good! It means properties are being read but are empty. Check your property file.

**Issue**: Properties work in one environment but not another
- **Solution**: Check for profile-specific property files or environment variables overriding your settings

### 10. YAML Format

If using `application.yml`, ensure proper indentation:

```yaml
fractalhive:
  keycloak:
    server-url: https://keycloak.example.com
    realm: your-realm
    client-id: your-client-id
    client-secret: your-client-secret
```

### Still Not Working?

1. Check application startup logs for errors
2. Verify the starter was built and installed correctly: `mvn clean install`
3. Ensure your application is using the correct version of the starter
4. Try restarting your IDE/application server
5. Check if there are any Spring Boot configuration exclusions in your main application class
