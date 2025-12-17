# Deployment Guide

This guide provides detailed instructions for deploying the AuthApp application in various environments.

## Table of Contents
- [Development Environment](#development-environment)
- [Production Environment](#production-environment)
- [Docker Deployment](#docker-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Cloud Deployment](#cloud-deployment)

## Development Environment

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

## Production Environment

### Prerequisites
- Java 17+ JRE
- PostgreSQL or MySQL database
- Nginx or Apache web server
- SSL certificate for HTTPS

### Backend Production Setup

1. **Configure Production Database**

Edit `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://your-db-host:5432/authdb
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.jpa.hibernate.ddl-auto=validate
```

2. **Build Production JAR**
```bash
cd backend
mvn clean package -DskipTests
```

3. **Run as System Service**

Create `/etc/systemd/system/authapp.service`:
```ini
[Unit]
Description=AuthApp Backend Service
After=network.target

[Service]
Type=simple
User=authapp
WorkingDirectory=/opt/authapp
ExecStart=/usr/bin/java -jar /opt/authapp/authapp-backend-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable authapp
sudo systemctl start authapp
```

### Frontend Production Setup

1. **Build for Production**
```bash
cd frontend
npm run build -- --configuration production
```

2. **Configure Nginx**

Create `/etc/nginx/sites-available/authapp`:
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;

    root /var/www/authapp;
    index index.html;

    # Frontend
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

3. **Deploy Frontend Files**
```bash
sudo cp -r dist/frontend/* /var/www/authapp/
sudo chown -R www-data:www-data /var/www/authapp
```

4. **Enable and Restart Nginx**
```bash
sudo ln -s /etc/nginx/sites-available/authapp /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Docker Deployment

### Backend Dockerfile

Create `backend/Dockerfile`:
```dockerfile
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/authapp-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

Create `frontend/Dockerfile`:
```dockerfile
FROM node:18 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Create `frontend/nginx.conf`:
```nginx
server {
    listen 80;
    server_name localhost;
    
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker Compose

Create `docker-compose.yml`:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: authdb
      POSTGRES_USER: authapp
      POSTGRES_PASSWORD: changeme
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authdb
      SPRING_DATASOURCE_USERNAME: authapp
      SPRING_DATASOURCE_PASSWORD: changeme
    depends_on:
      - postgres
    restart: unless-stopped

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres-data:
```

Build and run:
```bash
docker-compose up -d
```

## Kubernetes Deployment

### Backend Deployment

Create `k8s/backend-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: authapp-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: authapp-backend
  template:
    metadata:
      labels:
        app: authapp-backend
    spec:
      containers:
      - name: backend
        image: your-registry/authapp-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: authapp-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: authapp-secrets
              key: db-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: authapp-secrets
              key: db-password
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: authapp-backend-service
spec:
  selector:
    app: authapp-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

### Frontend Deployment

Create `k8s/frontend-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: authapp-frontend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: authapp-frontend
  template:
    metadata:
      labels:
        app: authapp-frontend
    spec:
      containers:
      - name: frontend
        image: your-registry/authapp-frontend:latest
        ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: authapp-frontend-service
spec:
  selector:
    app: authapp-frontend
  ports:
  - port: 80
    targetPort: 80
  type: LoadBalancer
```

### Secrets

Create `k8s/secrets.yaml`:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: authapp-secrets
type: Opaque
stringData:
  db-url: jdbc:postgresql://postgres:5432/authdb
  db-username: authapp
  db-password: your-secure-password
```

Deploy to Kubernetes:
```bash
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
```

## Cloud Deployment

### AWS Deployment

#### Using Elastic Beanstalk

1. Install AWS CLI and EB CLI
2. Initialize Elastic Beanstalk:
```bash
cd backend
eb init -p java-17 authapp-backend
eb create authapp-backend-env
```

#### Using ECS

1. Push Docker images to ECR
2. Create ECS task definitions
3. Create ECS services
4. Configure Application Load Balancer

### Azure Deployment

#### Using Azure App Service

1. Create App Service:
```bash
az webapp create \
  --resource-group authapp-rg \
  --plan authapp-plan \
  --name authapp-backend \
  --runtime "JAVA:17-java17"
```

2. Deploy:
```bash
cd backend
mvn clean package
az webapp deploy \
  --resource-group authapp-rg \
  --name authapp-backend \
  --src-path target/authapp-backend-1.0.0.jar
```

### Google Cloud Platform

#### Using Cloud Run

1. Build and push image:
```bash
gcloud builds submit --tag gcr.io/PROJECT_ID/authapp-backend
```

2. Deploy:
```bash
gcloud run deploy authapp-backend \
  --image gcr.io/PROJECT_ID/authapp-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## Post-Deployment Checklist

- [ ] Change default admin password
- [ ] Configure production JWT secret
- [ ] Set up SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure monitoring and alerting
- [ ] Set up log aggregation
- [ ] Enable HTTPS redirect
- [ ] Configure rate limiting
- [ ] Test all authentication methods
- [ ] Verify fraud detection is working
- [ ] Set up automated security updates
- [ ] Document deployment-specific configurations

## Monitoring

### Application Health Checks

Backend health endpoint:
```
GET http://your-domain.com/api/actuator/health
```

### Logging

Configure centralized logging with ELK Stack, Splunk, or cloud-native solutions.

### Metrics

Monitor key metrics:
- Request rate
- Response time
- Error rate
- Active sessions
- Login success/failure rate
- Account lock events

## Troubleshooting

### Backend Issues

1. Check logs:
```bash
journalctl -u authapp -f
```

2. Verify database connection:
```bash
psql -h localhost -U authapp -d authdb
```

### Frontend Issues

1. Check Nginx logs:
```bash
tail -f /var/log/nginx/error.log
```

2. Verify API connectivity from browser console

### Common Issues

**Issue**: CORS errors
**Solution**: Check CORS configuration in SecurityConfig.java

**Issue**: JWT token expired
**Solution**: Adjust token expiration time in application.properties

**Issue**: Database connection failed
**Solution**: Verify database credentials and network connectivity

## Rollback Procedure

### Backend Rollback
```bash
sudo systemctl stop authapp
sudo cp /opt/authapp/backup/authapp-backend-previous.jar /opt/authapp/authapp-backend-1.0.0.jar
sudo systemctl start authapp
```

### Frontend Rollback
```bash
sudo rm -rf /var/www/authapp/*
sudo cp -r /var/www/authapp-backup/* /var/www/authapp/
sudo systemctl reload nginx
```

## Security Hardening

1. **Firewall Configuration**
```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

2. **Fail2Ban for Brute Force Protection**
```bash
sudo apt-get install fail2ban
# Configure /etc/fail2ban/jail.local
```

3. **Regular Security Updates**
```bash
sudo apt-get update
sudo apt-get upgrade
```

4. **Database Security**
- Use strong passwords
- Limit database access to localhost or specific IPs
- Enable SSL for database connections
- Regular backups

## Support

For deployment issues, please check:
- Application logs
- System logs
- Database logs
- Network connectivity
- Security configurations

Contact: Create an issue in the GitHub repository
