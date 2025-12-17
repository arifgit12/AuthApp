# AuthApp - Enterprise Authentication Application

A comprehensive enterprise-grade authentication application built with Spring Boot and Angular, supporting multiple authentication methods including JWT, Basic Auth, LDAP, and Keycloak SSO.

## 🚀 Features

### Authentication Methods
- **JWT Authentication**: Stateless token-based authentication
- **Basic Authentication**: Simple username/password authentication  
- **LDAP Integration**: Enterprise directory integration (configurable)
- **Keycloak SSO**: Single Sign-On integration (configurable)

### Security Features
- **Multi-factor Authentication Ready**: Extensible architecture for MFA
- **Role-Based Access Control (RBAC)**: Dynamic role and privilege system
- **Fraud Detection**: Real-time monitoring of login attempts
  - IP-based tracking
  - Failed login attempt monitoring
  - Automatic account locking
  - Risk score calculation
  - Suspicious activity detection
- **Security Best Practices**:
  - CSRF protection
  - CORS configuration
  - Secure password hashing (BCrypt)
  - JWT token expiration
  - Security headers
  - Audit logging

### UI/UX
- Modern responsive design using Bootstrap 5 and Tabler
- Clean and intuitive user interface
- Dashboard with user profile and authentication status
- Multi-auth method selector

## 📋 Prerequisites

### Backend
- Java 17 or higher
- Maven 3.6+
- (Optional) PostgreSQL for production
- (Optional) LDAP server for LDAP authentication
- (Optional) Keycloak server for SSO

### Frontend
- Node.js 18+ and npm 9+
- Angular CLI 17

## 🛠️ Installation & Setup

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Configure the database and authentication methods in `src/main/resources/application.properties`:
   - For development, H2 in-memory database is pre-configured
   - For production, configure PostgreSQL or your preferred database
   - Enable LDAP by setting `app.ldap.enabled=true` and configuring LDAP properties
   - Enable Keycloak by setting `keycloak.enabled=true` and configuring Keycloak properties

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

#### Default Users

The application comes with pre-configured users:

| Username | Password | Role  | Description |
|----------|----------|-------|-------------|
| admin    | admin123 | ADMIN | Full access |
| user     | user123  | USER  | Limited access |

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Run the development server:
```bash
npm start
```

The frontend will start on `http://localhost:4200`

4. For production build:
```bash
npm run build
```

## 🔧 Configuration

### JWT Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
# JWT secret key (change in production!)
app.jwt.secret=your-secret-key-here

# JWT expiration time in milliseconds (24 hours)
app.jwt.expiration=86400000
```

### LDAP Configuration

```properties
app.ldap.enabled=true
spring.ldap.urls=ldap://your-ldap-server:389
spring.ldap.base=dc=example,dc=com
spring.ldap.username=cn=admin,dc=example,dc=com
spring.ldap.password=your-password
spring.ldap.user-dn-pattern=uid={0},ou=people
```

### Keycloak Configuration

```properties
keycloak.enabled=true
keycloak.realm=your-realm
keycloak.auth-server-url=http://your-keycloak-server:8180/auth
keycloak.resource=your-client-id
keycloak.credentials.secret=your-client-secret
```

### Fraud Detection Configuration

```properties
# Maximum failed login attempts before locking account
app.security.max-failed-attempts=5

# Account lockout duration in minutes
app.security.lockout-duration-minutes=30

# Time window for fraud detection in minutes
app.security.fraud-detection-window-minutes=60
```

## 🏗️ Architecture

### Backend Architecture

```
backend/
├── src/main/java/com/authapp/
│   ├── config/              # Configuration classes
│   │   ├── SecurityConfig.java
│   │   └── DataInitializer.java
│   ├── controller/          # REST controllers
│   │   └── AuthController.java
│   ├── dto/                 # Data Transfer Objects
│   ├── model/               # JPA entities
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Privilege.java
│   │   └── LoginAttempt.java
│   ├── repository/          # Data repositories
│   ├── security/            # Security implementations
│   │   ├── auth/            # Authentication strategies
│   │   │   ├── AuthenticationStrategy.java
│   │   │   ├── BasicAuthenticationStrategy.java
│   │   │   ├── JwtAuthenticationStrategy.java
│   │   │   └── LdapAuthenticationStrategy.java
│   │   └── jwt/             # JWT utilities
│   └── service/             # Business logic
│       ├── AuthenticationService.java
│       ├── FraudDetectionService.java
│       └── UserDetailsServiceImpl.java
```

### Frontend Architecture

```
frontend/src/app/
├── components/
│   ├── login/               # Login component
│   ├── register/            # Registration component
│   ├── dashboard/           # User dashboard
│   └── navbar/              # Navigation bar
├── services/
│   ├── auth.service.ts      # Authentication service
│   └── auth.interceptor.ts  # HTTP interceptor
├── guards/
│   └── auth.guard.ts        # Route guard
└── models/
    └── auth.model.ts        # TypeScript interfaces
```

## 🔐 API Endpoints

### Authentication

- `POST /api/auth/login` - User login
  ```json
  {
    "username": "user",
    "password": "password",
    "authMethod": "JWT|BASIC|LDAP|KEYCLOAK"
  }
  ```

- `POST /api/auth/register` - User registration
  ```json
  {
    "username": "newuser",
    "password": "password",
    "email": "user@example.com",
    "fullName": "John Doe"
  }
  ```

- `POST /api/auth/logout` - User logout

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 🚀 Deployment

### Backend Deployment

1. Build the JAR file:
```bash
cd backend
mvn clean package
```

2. Run the JAR:
```bash
java -jar target/authapp-backend-1.0.0.jar
```

### Frontend Deployment

1. Build for production:
```bash
cd frontend
npm run build
```

2. Deploy the `dist/frontend` directory to your web server (nginx, Apache, etc.)

### Docker Deployment (Optional)

Create `Dockerfile` in backend directory:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/authapp-backend-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Create `Dockerfile` in frontend directory:
```dockerfile
FROM node:18 as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
```

## 🔒 Security Considerations

1. **Change Default Credentials**: Always change default admin credentials in production
2. **Use Strong JWT Secret**: Generate a strong secret key for JWT signing
3. **Enable HTTPS**: Always use HTTPS in production
4. **Database Security**: Use strong database passwords and limit access
5. **Regular Updates**: Keep all dependencies up to date
6. **Monitor Logs**: Regularly check security audit logs
7. **Rate Limiting**: Consider adding API rate limiting for production

## 📊 Monitoring & Logging

The application logs important security events:
- Login attempts (successful and failed)
- Account locks
- Suspicious activities
- Authentication method changes

Logs can be configured in `application.properties`:
```properties
logging.level.com.authapp=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For issues and questions:
- Create an issue in the GitHub repository
- Check the documentation
- Review the API endpoints

## 🔄 Roadmap

- [ ] Multi-factor authentication (MFA)
- [ ] OAuth2 integration (Google, GitHub, etc.)
- [ ] Password reset functionality
- [ ] Email verification
- [ ] Session management dashboard
- [ ] Advanced fraud detection with machine learning
- [ ] API rate limiting
- [ ] Comprehensive audit trail
- [ ] User activity monitoring
- [ ] Admin panel for user management

## 📚 Technology Stack

### Backend
- Spring Boot 3.2.1
- Spring Security
- Spring Data JPA
- JWT (JJWT 0.12.3)
- Keycloak 23.0.3
- H2/PostgreSQL
- Lombok
- Maven

### Frontend
- Angular 17
- TypeScript
- Bootstrap 5
- Tabler UI
- RxJS
- HttpClient
