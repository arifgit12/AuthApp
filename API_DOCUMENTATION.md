# API Documentation

## Base URL
```
Development: http://localhost:8080
Production: https://your-domain.com
```

## Authentication

All protected endpoints require authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Endpoints

### 1. User Registration

Register a new user account.

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "fullName": "string"
}
```

**Response:** `200 OK`
```json
"User registered successfully"
```

**Error Responses:**
- `400 Bad Request`: Username or email already exists
```json
"Username already exists"
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123",
    "email": "john@example.com",
    "fullName": "John Doe"
  }'
```

---

### 2. User Login

Authenticate a user and receive a JWT token.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "username": "string",
  "password": "string",
  "authMethod": "JWT|BASIC|LDAP|KEYCLOAK"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "privileges": ["READ_PRIVILEGE"],
  "authMethod": "JWT"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid credentials
```json
"Authentication failed: Bad credentials"
```
- `403 Forbidden`: Account locked
```json
"Account is locked. Please contact administrator."
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123",
    "authMethod": "JWT"
  }'
```

---

### 3. User Logout

Logout the current user.

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Authorization: Bearer <your-jwt-token>
```

**Response:** `200 OK`
```json
"User logged out successfully"
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Authentication Methods

### JWT Authentication

JWT (JSON Web Token) is the default authentication method. After successful login, you receive a token that must be included in all subsequent requests.

**Features:**
- Stateless authentication
- Token expiration (configurable, default 24 hours)
- Includes user claims (username, roles, privileges)

**Usage:**
```javascript
// Include in every request header
headers: {
  'Authorization': 'Bearer ' + token
}
```

### Basic Authentication

Traditional username/password authentication for each request.

**Usage:**
```bash
curl -X GET http://localhost:8080/api/protected \
  -u username:password
```

### LDAP Authentication

Authenticate against an LDAP directory server.

**Configuration Required:**
```properties
app.ldap.enabled=true
spring.ldap.urls=ldap://ldap-server:389
spring.ldap.base=dc=example,dc=com
```

**Usage:**
Set `authMethod` to `LDAP` in login request.

### Keycloak SSO

Single Sign-On using Keycloak identity provider.

**Configuration Required:**
```properties
keycloak.enabled=true
keycloak.realm=your-realm
keycloak.auth-server-url=http://keycloak:8180/auth
keycloak.resource=authapp-client
```

**Usage:**
1. Redirect user to Keycloak login page
2. User authenticates with Keycloak
3. Keycloak redirects back with token
4. Use token for API requests

---

## Security Features

### Fraud Detection

The system automatically monitors and detects:
- Multiple failed login attempts from same username
- Multiple failed login attempts from same IP
- Rapid successive login attempts
- Suspicious patterns

**Automatic Actions:**
- Account locking after configurable failed attempts (default: 5)
- Temporary IP blocking
- Risk score calculation
- Security event logging

**Configuration:**
```properties
app.security.max-failed-attempts=5
app.security.lockout-duration-minutes=30
app.security.fraud-detection-window-minutes=60
```

### Role-Based Access Control (RBAC)

**Default Roles:**
- `USER`: Basic access
- `MODERATOR`: Moderate access
- `ADMIN`: Full access

**Privileges:**
- `READ_PRIVILEGE`: Read access
- `WRITE_PRIVILEGE`: Write access
- `DELETE_PRIVILEGE`: Delete access
- `ADMIN_PRIVILEGE`: Administrative access

**Usage in Code:**
```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAuthority('WRITE_PRIVILEGE')")
```

---

## Error Codes

| Status Code | Description |
|------------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication failed |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource already exists |
| 500 | Internal Server Error |

---

## Rate Limiting

To prevent abuse, API endpoints are rate-limited:
- Login endpoint: 5 requests per minute per IP
- Registration endpoint: 3 requests per hour per IP
- Other endpoints: 100 requests per minute per user

**Rate Limit Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640000000
```

---

## Pagination

For endpoints that return lists, use pagination parameters:

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Items per page (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `username,asc`)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/users?page=0&size=20&sort=username,asc" \
  -H "Authorization: Bearer <token>"
```

---

## Webhooks

Configure webhooks to receive notifications for security events:

**Supported Events:**
- `user.login.success`
- `user.login.failed`
- `user.account.locked`
- `user.suspicious.activity`

**Webhook Payload:**
```json
{
  "event": "user.login.failed",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "username": "johndoe",
    "ipAddress": "192.168.1.100",
    "reason": "Invalid password"
  }
}
```

---

## Best Practices

1. **Always use HTTPS in production**
2. **Store tokens securely** (sessionStorage, not localStorage)
3. **Implement token refresh** before expiration
4. **Validate all inputs** on client and server
5. **Handle errors gracefully**
6. **Log security events**
7. **Monitor failed login attempts**
8. **Use strong passwords**
9. **Enable CORS properly**
10. **Keep dependencies updated**

---

## Examples

### Complete Login Flow

```javascript
// 1. Login
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: 'johndoe',
    password: 'SecurePass123',
    authMethod: 'JWT'
  })
})
.then(response => response.json())
.then(data => {
  // 2. Store token
  sessionStorage.setItem('token', data.token);
  sessionStorage.setItem('user', JSON.stringify(data));
  
  // 3. Use token for subsequent requests
  return fetch('http://localhost:8080/api/protected', {
    headers: {
      'Authorization': 'Bearer ' + data.token
    }
  });
})
.catch(error => console.error('Error:', error));
```

### Handle Token Expiration

```javascript
// Interceptor to handle 401 errors
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response.status === 401) {
      // Token expired, redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## Testing

### Using cURL

**Register User:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123","email":"test@example.com","fullName":"Test User"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123","authMethod":"JWT"}'
```

### Using Postman

1. Import the collection from `postman/AuthApp.postman_collection.json`
2. Set environment variables:
   - `base_url`: `http://localhost:8080`
   - `token`: (automatically set after login)
3. Run the collection

---

## Support

For API issues or questions:
- GitHub Issues: https://github.com/your-repo/AuthApp/issues
- Documentation: https://github.com/your-repo/AuthApp/wiki
- Email: support@example.com

---

## Changelog

### Version 1.0.0
- Initial release
- JWT authentication
- Basic authentication
- LDAP integration
- Keycloak SSO support
- Fraud detection
- RBAC implementation
