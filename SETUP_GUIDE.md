# KuberFashion Backend Setup Guide

## 🚀 Complete Setup Instructions

### Prerequisites
- **Java 17 or higher** (Java 21 recommended)
- **Maven 3.6+**
- **PostgreSQL Database** (or Supabase account)
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code)

### Step 1: Install Java and Maven

#### Windows:
1. Download Java 17+ from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
2. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
3. Set environment variables:
   ```cmd
   JAVA_HOME=C:\Program Files\Java\jdk-17
   MAVEN_HOME=C:\Program Files\Apache\maven
   PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
   ```

#### Verify Installation:
```bash
java -version
mvn -version
```

### Step 2: Database Setup

#### Option A: Use Supabase (Recommended - Free)
1. Go to [supabase.com](https://supabase.com)
2. Create a free account
3. Create a new project
4. Go to Settings → Database
5. Copy your connection details

#### Option B: Local PostgreSQL
1. Install PostgreSQL from [postgresql.org](https://www.postgresql.org/download/)
2. Create a database named `kuberfashion`
3. Note your username/password

### Step 3: Configure Database Connection

Update `src/main/resources/application.properties`:

```properties
# Database Configuration (UPDATE THESE VALUES)
spring.datasource.url=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# JWT Configuration (CHANGE THIS SECRET IN PRODUCTION)
jwt.secret=kuberfashion-secret-key-2025-secure-token-change-in-production
jwt.expiration=86400000

# CORS Configuration
cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging Configuration
logging.level.com.kuberfashion.backend=INFO
logging.level.org.springframework.security=INFO

# Server Configuration
server.port=8080
```

### Step 4: Build and Run

#### Method 1: Using Maven Command Line
```bash
# Navigate to backend directory
cd backend

# Clean and compile
mvn clean compile

# Run the application
mvn spring-boot:run
```

#### Method 2: Using IDE
1. Open the project in your IDE
2. Right-click on `KuberFashionApplication.java`
3. Select "Run" or "Debug"

#### Method 3: Build JAR and Run
```bash
# Build JAR file
mvn clean package

# Run the JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Step 5: Verify Setup

1. **Check if server is running:**
   - Open browser: `http://localhost:8080`
   - You should see a login page or 401 error (this is normal)

2. **Test API endpoints:**
   ```bash
   # Get all categories
   curl http://localhost:8080/api/categories
   
   # Get all products
   curl http://localhost:8080/api/products
   ```

3. **Check database:**
   - Tables should be automatically created
   - Sample data should be loaded

### Step 6: Test Authentication

#### Register a new user:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "password": "password123"
  }'
```

#### Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

### Step 7: Pre-loaded Test Data

The application automatically creates:

#### Test Users:
- **Admin:** `admin@kuberfashion.com` / `admin123`
- **User:** `test@kuberfashion.com` / `test123`

#### Categories:
- Men's Fashion
- Women's Fashion
- Footwear
- Accessories
- Kids Fashion
- Sports & Active

#### Sample Products:
- 8 products across different categories
- With realistic prices, images, and descriptions

## 🔧 Troubleshooting

### Common Issues:

#### 1. "Port 8080 already in use"
```bash
# Kill process on port 8080 (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in application.properties
server.port=8081
```

#### 2. Database Connection Failed
- Check if PostgreSQL is running
- Verify connection details in `application.properties`
- Ensure database exists
- Check firewall settings

#### 3. JWT Secret Key Error
- Ensure JWT secret is at least 32 characters
- Update `jwt.secret` in `application.properties`

#### 4. Maven Build Fails
```bash
# Clear Maven cache
mvn clean
mvn dependency:purge-local-repository

# Reload dependencies
mvn dependency:resolve
```

#### 5. Java Version Issues
```bash
# Check Java version
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java
```

### Logs Location:
- Console output shows all logs
- Check for ERROR or WARN messages
- Database connection logs appear on startup

## 🔐 Security Configuration

### JWT Token:
- Default expiration: 24 hours
- Change `jwt.secret` for production
- Tokens are stateless

### Password Security:
- BCrypt encryption
- Minimum 6 characters required
- No password complexity enforced (add if needed)

### CORS:
- Configured for `localhost:3000` (React frontend)
- Add production domains as needed

## 📱 Frontend Integration

### API Base URL:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

### Authentication Headers:
```javascript
const token = localStorage.getItem('token');
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};
```

### Sample Frontend Service:
```javascript
// Login service
const login = async (email, password) => {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  return response.json();
};
```

## 🎯 Next Steps

1. **Test all endpoints** using Postman or curl
2. **Connect your React frontend** to the backend
3. **Customize the sample data** as needed
4. **Add additional features** if required
5. **Deploy to production** when ready

## 📞 Support

If you encounter any issues:
1. Check the console logs for errors
2. Verify database connection
3. Ensure all dependencies are installed
4. Check if ports are available
5. Review this setup guide again

The backend should now be fully functional and ready to integrate with your React frontend!
