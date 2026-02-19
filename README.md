# FractalHive Keycloak Spring Boot Starter

A reusable Spring Boot starter project that encapsulates all Keycloak authentication and authorization functionality. This starter enables any Spring Boot application to integrate Keycloak authentication with minimal configuration.

## Features

- **Multi-tenancy Support**: Each application can configure its own realm and client
- **Complete Isolation**: Applications don't interfere with each other
- **Flexible Authentication**: Supports both cookie-based and bearer token authentication
- **Dual Authentication Methods**: 
  - HTTP-only cookies for web applications
  - Bearer tokens for API clients
- **Role Management**: 
  - Full CRUD operations for realm roles
  - Client role support
  - Composite roles with sub-roles
  - Role assignment to users (realm and client roles)
- **User Management**: 
  - User registration
  - User lookup by ID or email
  - User update and deletion
- **Password Management**: 
  - Password reset request (sends email)
  - Password reset with new password
  - Password change for authenticated users
- **Auto-configuration**: Zero-code integration for basic use cases
- **Swagger/OpenAPI Documentation**: Interactive API documentation included
- **Customizable**: All components can be overridden by consuming applications

## Quick Start for New Product

When adding a new product/client to your organization, follow these simple steps:

### Step 1: Create Keycloak Client

1. Log in to your Keycloak Admin Console
2. Navigate to your realm (e.g., `fractalhive`)
3. Go to **Clients** → **Create client**
4. Set **Client ID** (e.g., `product-xyz`) - this MUST be unique
5. Enable **Client authentication** (confidential client)
6. Save and go to **Credentials** tab
7. Copy the **Client secret**

### Step 2: Add Starter Dependency

Add the starter to your new product's `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 3: Configure Application Properties

Copy this template to your `application.properties` and fill in your values:

```properties
# ============================================
# Keycloak Configuration for New Product
# ============================================
# Replace the values below with your Keycloak client details

# Required: Keycloak Server Configuration
fractalhive.keycloak.server-url=https://your-keycloak-server.com
fractalhive.keycloak.realm=your-realm-name
fractalhive.keycloak.client-id=your-product-client-id
fractalhive.keycloak.client-secret=your-product-client-secret
fractalhive.keycloak.resource-id=${fractalhive.keycloak.client-id}

# Required: Spring Security JWT Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=${fractalhive.keycloak.server-url}/realms/${fractalhive.keycloak.realm}

# Optional: Cookie Configuration (for cookie-based auth)
fractalhive.keycloak.cookie.secure=true
fractalhive.keycloak.cookie.domain=.yourdomain.com
fractalhive.keycloak.cookie.same-site=None

# Optional: Admin Realm (for admin authentication)
# If not specified, uses the same realm as your target realm
# Only set to "master" if you specifically need master realm authentication
# fractalhive.keycloak.admin-realm=fractalhive

# Optional: Admin Client (for role/user management)
# If not specified, uses the main client-id and client-secret
# fractalhive.keycloak.admin.client-id=admin-cli
# fractalhive.keycloak.admin.client-secret=admin-secret
```

### Step 4: Start Your Application

That's it! Your application now has:
- ✅ Authentication endpoints at `/auth/*`
- ✅ JWT token validation (both cookie and bearer token)
- ✅ Role-based authorization
- ✅ User management APIs
- ✅ Password management (reset, change)
- ✅ Role management (realm and client roles, composite roles)
- ✅ Swagger/OpenAPI documentation at `/swagger-ui.html`

### Example Configuration

**Product ABC:**
```properties
fractalhive.keycloak.server-url=https://fhkeycloakazure.azurewebsites.net
fractalhive.keycloak.realm=fractalhive
fractalhive.keycloak.client-id=product-abc
fractalhive.keycloak.client-secret=abc-secret-123
fractalhive.keycloak.resource-id=product-abc
spring.security.oauth2.resourceserver.jwt.issuer-uri=${fractalhive.keycloak.server-url}/realms/${fractalhive.keycloak.realm}
```

**Product XYZ:**
```properties
fractalhive.keycloak.server-url=https://fhkeycloakazure.azurewebsites.net
fractalhive.keycloak.realm=fractalhive
fractalhive.keycloak.client-id=product-xyz
fractalhive.keycloak.client-secret=xyz-secret-456
fractalhive.keycloak.resource-id=product-xyz
spring.security.oauth2.resourceserver.jwt.issuer-uri=${fractalhive.keycloak.server-url}/realms/${fractalhive.keycloak.realm}
```

> **Note:** Both products use the same Keycloak server and realm, but have different `client-id` and `client-secret` for complete isolation.

### Configuration Validation

The starter automatically validates required properties at startup. If any required property is missing, you'll get a clear error message:

```
Keycloak configuration is missing required property: 'fractalhive.keycloak.client-id'. 
This MUST be unique per application. Please add this property to your application.properties or application.yml
```

## Project Structure

```
fractalhive-spring-boot-starter-keycloak/
├── pom.xml
├── README.md
└── src/main/java/com/fractalhive/keycloak/
    ├── autoconfigure/
    │   ├── KeycloakAuthAutoConfiguration.java
    │   └── KeycloakAuthProperties.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── JwtAuthConverter.java
    │   ├── JwtCookieAuthenticationFilter.java
    │   ├── AuthorizationHeaderRequestWrapper.java
    │   ├── WebClientConfig.java
    │   └── OpenApiConfig.java
    ├── controller/
    │   └── KeycloakAuthController.java
    ├── service/
    │   ├── KeycloakAuthService.java
    │   ├── KeycloakUserService.java
    │   ├── KeycloakRoleService.java
    │   └── KeycloakPasswordService.java
    ├── dto/
    │   ├── LoginRequest.java
    │   ├── LoginResponse.java
    │   ├── RegisterRequest.java
    │   ├── RegisterResponse.java
    │   ├── UserInfoResponse.java
    │   ├── RoleRequest.java
    │   ├── RoleResponse.java
    │   ├── AssignRoleRequest.java
    │   └── PasswordResetRequest.java
    ├── exception/
    │   └── KeycloakAuthException.java
    └── util/
        └── SecurityUtils.java
```

## Building the Starter

### Step 1: Build and Install to Local Maven Repository

Navigate to the starter project directory:

```bash
cd fractalhive-spring-boot-starter-keycloak
```

Build and install to local Maven repository:

```bash
# Development build (fast, skip tests)
mvn clean install -Pdev

# Release build (full, with all tests)
mvn clean install -Prelease

# Standard build (default)
mvn clean install
```

This will install the JAR to your local Maven repository at:
- `~/.m2/repository/com/fractalhive/fractalhive-spring-boot-starter-keycloak/1.0.0/`

## Using the Starter in Your Application

### Step 1: Add Dependency

Add the starter dependency to your Spring Boot application's `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Configure Keycloak Properties

Add configuration to your `application.properties`:

```properties
# Keycloak Server Configuration
fractalhive.keycloak.server-url=https://fhkeycloakazure.azurewebsites.net
fractalhive.keycloak.realm=fractalhive
fractalhive.keycloak.client-id=your-client-id
fractalhive.keycloak.client-secret=your-client-secret
fractalhive.keycloak.resource-id=your-client-id

# Cookie Configuration
fractalhive.keycloak.cookie.secure=true
fractalhive.keycloak.cookie.domain=.example.com
fractalhive.keycloak.cookie.same-site=None

# Optional: Admin Realm Configuration
# If not specified, uses the same realm as your target realm
# fractalhive.keycloak.admin-realm=fractalhive

# Optional: Admin Client Configuration (for role/user management)
# If not specified, uses the main client-id and client-secret
# fractalhive.keycloak.admin.client-id=admin-cli
# fractalhive.keycloak.admin.client-secret=admin-secret

# Spring Security OAuth2 Resource Server JWT Configuration
# This is required for JWT token validation
spring.security.oauth2.resourceserver.jwt.issuer-uri=${fractalhive.keycloak.server-url}/realms/${fractalhive.keycloak.realm}

# Public Endpoints (optional, defaults provided)
fractalhive.keycloak.public-endpoints=/auth/login,/auth/register,/auth/refresh,/swagger-ui/**,/v3/api-docs/**
```

### Step 3: Start Your Application

When you start your application:

1. Spring Boot scans for auto-configuration classes
2. Finds `KeycloakAuthAutoConfiguration` (via auto-configuration imports)
3. Reads your `application.properties` configuration
4. Creates all necessary beans (services, controllers, security config)
5. Your application now has all auth endpoints available at `/auth/*`

## Configuration Properties

### KeycloakAuthProperties

All configuration properties are prefixed with `fractalhive.keycloak`:

| Property | Description | Required | Default |
|----------|-------------|----------|---------|
| `server-url` | Base Keycloak server URL | Yes | - |
| `realm` | Realm name | Yes | - |
| `client-id` | Client ID (MUST be unique per app) | Yes | - |
| `client-secret` | Client secret (MUST be unique per app) | Yes | - |
| `resource-id` | Resource ID for role extraction | No | Same as `client-id` |
| `cookie.secure` | Enable secure cookies | No | `true` |
| `cookie.domain` | Cookie domain | No | - |
| `cookie.same-site` | SameSite policy | No | `None` |
| `admin.client-id` | Admin client ID | No | Same as `client-id` |
| `admin.client-secret` | Admin client secret | No | Same as `client-secret` |
| `admin-realm` | Admin realm for authentication | No | Same as `realm` |
| `public-endpoints` | Public endpoints array | No | Default endpoints |

## API Endpoints

All endpoints are documented with Swagger/OpenAPI. Access the interactive API documentation at `/swagger-ui.html` when your application is running.

### Authentication Endpoints

- `POST /auth/login` - User login (returns tokens in HTTP-only cookies)
- `POST /auth/logout` - User logout (invalidates refresh token and clears cookies)
- `POST /auth/refresh` - Refresh access token using refresh token from cookies
- `GET /auth/me` - Get current user info (requires authentication)

**Authentication Methods:**
- **Cookie-based**: Tokens are automatically stored in HTTP-only cookies after login
- **Bearer token**: Include `Authorization: Bearer <token>` header for API calls

### User Management Endpoints

- `POST /auth/register` - Register new user (public endpoint)

### Password Management Endpoints

- `POST /auth/password/reset-request?email={email}` - Request password reset (sends email with reset link)
- `POST /auth/password/reset` - Reset password with new password (requires email and new password)
- `POST /auth/password/change?newPassword={password}` - Change password (authenticated user)

### Role Management Endpoints

- `GET /auth/roles` - List all realm roles (Note: Currently returns empty list - full implementation pending)
- `POST /auth/roles` - Create realm role
- `GET /auth/roles/{roleName}` - Get realm role details
- `PUT /auth/roles/{roleName}` - Update realm role
- `DELETE /auth/roles/{roleName}` - Delete realm role
- `POST /auth/roles/{roleName}/composite` - Create composite role with sub-roles
- `POST /auth/roles/{roleName}/sub-roles?subRoleName={name}` - Add sub-role to composite role
- `DELETE /auth/roles/{roleName}/sub-roles/{subRoleName}` - Remove sub-role from composite role

**Note:** The starter supports both realm roles and client roles. When assigning roles to users, you can specify `isRealmRole=true` (default) for realm roles or `isRealmRole=false` for client roles.

### User Role Assignment Endpoints

- `POST /auth/users/{userId}/roles?isRealmRole={true|false}` - Assign role(s) to user (supports both realm and client roles)
- `DELETE /auth/users/{userId}/roles/{roleName}?isRealmRole={true|false}` - Remove role from user
- `GET /auth/users/{userId}/roles` - Get all realm roles for a user

## Multi-Application Configuration & Isolation

### How It Works

**Each Spring Boot application is completely isolated** - the starter is a library dependency that each application includes independently. Each application configures its own Keycloak realm and client in its own `application.properties` file.

### Example Scenario

**Product ABC (Existing Application)**

```properties
# application.properties in abc-product project
fractalhive.keycloak.server-url=https://fhkeycloakazure.azurewebsites.net
fractalhive.keycloak.realm=fractalhive
fractalhive.keycloak.client-id=abc
fractalhive.keycloak.client-secret=abc-secret-123
fractalhive.keycloak.resource-id=abc
```

**Product XYZ (New Application - Months Later)**

```properties
# application.properties in xyz-product project
fractalhive.keycloak.server-url=https://fhkeycloakazure.azurewebsites.net
fractalhive.keycloak.realm=fractalhive
fractalhive.keycloak.client-id=xyz
fractalhive.keycloak.client-secret=xyz-secret-456
fractalhive.keycloak.resource-id=xyz
```

### Key Design Principles

1. **No Shared State**: Each application instance has its own Spring context with its own configuration
2. **Per-Application Configuration**: Configuration is read from each application's `application.properties` or `application.yml`
3. **Isolated Beans**: All services, controllers, and configurations are scoped to the application's Spring context
4. **Independent Operation**: Changes to one application's configuration don't affect others
5. **Same Keycloak Server, Different Clients**: All applications can use the same Keycloak server but with different realms/clients

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│         Centralized Keycloak Server                         │
│  https://fhkeycloakazure.azurewebsites.net                 │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Realm:       │  │ Realm:       │  │ Realm:       │    │
│  │ fractalhive  │  │ fractalhive  │  │ fractalhive  │    │
│  │              │  │              │  │              │    │
│  │ Client: abc  │  │ Client: xyz  │  │ Client: def  │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         │                    │                    │
    ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
    │ Product │         │ Product │         │ Product │
    │   ABC   │         │   XYZ   │         │   DEF   │
    │         │         │         │         │         │
    │ Uses:   │         │ Uses:   │         │ Uses:   │
    │ - abc   │         │ - xyz   │         │ - def   │
    │ client  │         │ client  │         │ client  │
    │         │         │         │         │         │
    │ Each has its own application.properties         │
    │ Each includes fractalhive-spring-boot-         │
    │   starter-keycloak as dependency                │
    └─────────┘         └─────────┘         └─────────┘
```

## Adding a New Application

1. **Create new Keycloak client** in Keycloak admin console (or via API)
2. **Add starter dependency** to new project's `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.fractalhive</groupId>
       <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```
3. **Configure in `application.properties`** with new client details
4. **No impact on existing applications** - they continue using their own configurations

## Maven Build Profiles

The starter project includes Maven profiles for different build scenarios:

### Development Profile (`-Pdev`)

- Fast builds for development
- Skip tests: `-DskipTests`
- Skip integration tests
- No Javadoc generation
- No artifact signing

**Usage:**
```bash
mvn clean install -Pdev
```

### Release Profile (`-Prelease`)

- Full production build
- Run all tests (unit + integration)
- Generate Javadoc
- Sign artifacts (if GPG configured)
- Generate source JARs

**Usage:**
```bash
mvn clean install -Prelease
```

### Default Profile (No profile specified)

- Standard build
- Run unit tests
- Skip integration tests (unless explicitly enabled)
- Generate Javadoc

**Usage:**
```bash
mvn clean install
```

## Authentication Flow

### Cookie-Based Authentication (Web Applications)

1. User calls `POST /auth/login` with credentials
2. Server authenticates with Keycloak and receives JWT tokens
3. Tokens are stored in HTTP-only cookies (`access_token` and `refresh_token`)
4. Subsequent requests automatically include cookies
5. `JwtCookieAuthenticationFilter` extracts token from cookie and validates it
6. User can call `POST /auth/refresh` to refresh the access token
7. User calls `POST /auth/logout` to invalidate tokens and clear cookies

### Bearer Token Authentication (API Clients)

1. User calls `POST /auth/login` with credentials
2. Server returns tokens in response body (or extract from cookies)
3. Client includes `Authorization: Bearer <access_token>` header in requests
4. Spring Security validates the JWT token using the configured issuer URI
5. User can call `POST /auth/refresh` with refresh token to get new access token

### Role Extraction

Roles are extracted from JWT tokens in two ways:
- **Realm roles**: From `realm_access.roles` claim
- **Client roles**: From `resource_access.{resource-id}.roles` claim

The `resource-id` defaults to your `client-id` but can be configured separately.

## Customization

All components can be overridden by consuming applications:

- **SecurityConfig**: Override `SecurityFilterChain` bean to customize security rules
- **CorsConfigurationSource**: Override `CorsConfigurationSource` bean to customize CORS
- **WebClient**: Override `WebClient` bean to customize HTTP client settings
- **Services**: All services are `@Service` beans that can be extended or replaced
- **JwtAuthConverter**: Override to customize JWT to Authentication conversion
- **JwtCookieAuthenticationFilter**: Override to customize cookie-based authentication

## Updating the Starter

When you make changes to the starter:

1. **Update the version** in starter's `pom.xml`:
   ```xml
   <version>1.0.1</version>  <!-- Increment version -->
   ```

2. **Rebuild and reinstall:**
   ```bash
   mvn clean install
   ```

3. **Update version in product application's `pom.xml`:**
   ```xml
   <dependency>
       <groupId>com.fractalhive</groupId>
       <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
       <version>1.0.1</version>  <!-- Update to new version -->
   </dependency>
   ```

4. **Rebuild product application:**
   ```bash
   mvn clean install
   ```

## Publishing to Maven Repository

For team/organization use, publish to:

### Private Maven Repository (Nexus, Artifactory, etc.)

```xml
<distributionManagement>
    <repository>
        <id>nexus</id>
        <url>https://nexus.yourcompany.com/repository/maven-releases</url>
    </repository>
</distributionManagement>
```

```bash
mvn clean deploy
```

### Maven Central (for public distribution)

- Follow Maven Central publishing guidelines
- Sign artifacts with GPG
- Use Sonatype OSSRH

## Requirements

- **Java 21+**
- **Spring Boot 3.5.7+**
- **Maven 3.6+**
- **Keycloak Server** (version 20+ recommended)

## Dependencies

The starter includes the following key dependencies:
- Spring Boot Starter Web
- Spring Boot Starter Security
- Spring Boot Starter OAuth2 Resource Server
- Spring WebFlux (for WebClient)
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- Jakarta Validation

## License

[Specify your license here]

## Support

For issues and questions, please contact [your support contact].
