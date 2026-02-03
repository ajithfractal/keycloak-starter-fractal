# How to Create Users - Step by Step Guide

## Quick Answer

To create a user, call the **`POST /auth/register`** endpoint with user details.

## Prerequisites

1. **Configure your application.properties** with Keycloak settings:
   ```properties
   fractalhive.keycloak.server-url=https://your-keycloak-server.com
   fractalhive.keycloak.realm=fractalhive
   fractalhive.keycloak.client-id=your-client-id
   fractalhive.keycloak.client-secret=your-client-secret
   ```

2. **Ensure your Keycloak client has admin permissions** - The client needs to be able to create users in Keycloak.

## Method 1: Using the REST API Endpoint

### Endpoint
```
POST http://localhost:8080/auth/register
```

### Request Body (JSON)
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Example: Using cURL
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Example: Using Postman
1. Method: `POST`
2. URL: `http://localhost:8080/auth/register`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
   ```json
   {
     "email": "user@example.com",
     "password": "SecurePassword123!",
     "firstName": "John",
     "lastName": "Doe"
   }
   ```

### Example: Using JavaScript/Fetch
```javascript
fetch('http://localhost:8080/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!',
    firstName: 'John',
    lastName: 'Doe'
  })
})
.then(response => response.json())
.then(data => console.log('User created:', data))
.catch(error => console.error('Error:', error));
```

### Response
```json
{
  "userId": "abc123-def456-ghi789",
  "email": "user@example.com",
  "message": "User registered successfully"
}
```

## Method 2: Using the Service Directly (In Your Code)

If you want to create users programmatically in your application code:

```java
import com.fractalhive.keycloak.service.KeycloakUserService;
import com.fractalhive.keycloak.dto.RegisterRequest;
import com.fractalhive.keycloak.dto.RegisterResponse;

@RestController
public class MyController {
    
    @Autowired
    private KeycloakUserService userService;
    
    @PostMapping("/create-user")
    public RegisterResponse createUser(@RequestBody RegisterRequest request) {
        return userService.register(request);
    }
}
```

## Field Requirements

| Field | Required | Validation | Example |
|-------|----------|------------|---------|
| `email` | Yes | Must be valid email format | `user@example.com` |
| `password` | Yes | Minimum 8 characters | `SecurePassword123!` |
| `firstName` | Yes | Cannot be blank | `John` |
| `lastName` | Yes | Cannot be blank | `Doe` |

## Important Notes

1. **Public Endpoint**: The `/auth/register` endpoint is public by default (no authentication required)
2. **Email as Username**: The email is used as both the username and email in Keycloak
3. **Email Verification**: New users are created with `emailVerified: false` - you may want to send a verification email
4. **Admin Token**: User creation requires an admin access token, which is automatically obtained using your configured client credentials

## Testing with Swagger UI

1. Start your application
2. Open: `http://localhost:8080/swagger-ui.html`
3. Find the **"Keycloak Authentication"** section
4. Click on **`POST /auth/register`**
5. Click **"Try it out"**
6. Fill in the request body:
   ```json
   {
     "email": "test@example.com",
     "password": "TestPassword123!",
     "firstName": "Test",
     "lastName": "User"
   }
   ```
7. Click **"Execute"**

## Troubleshooting

### Error: "User registration failed"
- Check that your `client-id` and `client-secret` are correct
- Verify the client has admin permissions in Keycloak
- Check that the realm name is correct

### Error: "User already exists"
- The email is already registered in Keycloak
- Use a different email or update the existing user

### Error: "Authentication failed" when getting admin token
- Verify `fractalhive.keycloak.server-url` is correct
- Check that `fractalhive.keycloak.realm` matches your Keycloak realm
- Ensure `fractalhive.keycloak.client-id` and `client-secret` are valid

## Next Steps After Creating a User

1. **User can login**: The user can now use `/auth/login` with their email and password
2. **Send verification email** (optional): Use the password service to send email verification
3. **Assign roles** (optional): Use `/auth/users/{userId}/roles` to assign roles to the user

## Example: Complete User Creation Flow

```bash
# 1. Create the user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "Password123!",
    "firstName": "New",
    "lastName": "User"
  }'

# Response: {"userId": "abc-123", "email": "newuser@example.com", "message": "User registered successfully"}

# 2. User can now login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "Password123!"
  }'
```
