# 🚀 KuberFashion Backend - Quick Start Guide

## ⚡ FASTEST WAY TO RUN

### Step 1: Install Prerequisites (5 minutes)
1. **Download Java 17+**: https://adoptium.net/temurin/releases/
2. **Download Maven**: https://maven.apache.org/download.cgi
3. **Extract both** and add to PATH

### Step 2: Run the Project (1 minute)
```bash
# Navigate to backend folder
cd backend

# Run this single Commands`
./run.bat
```

**OR manually:**
```bash
mvn clean compile
mvn spring-boot:run
```

### Step 3: Test (30 seconds)
Open browser: `http://localhost:8080/api/test/health`

You should see:
```json
{
  "success": true,
  "message": "Health check successful",
  "data": {
    "status": "UP",
    "timestamp": "2024-09-24T20:04:39",
    "message": "KuberFashion Backend is running successfully!",
    "version": "1.0.0"
  }
}
```

## 🔧 CONFIGURATION VALUES TO UPDATE

### 1. Database Configuration
**File:** `src/main/resources/application.properties`

**Current values (working with your Supabase):**
```properties
spring.datasource.url=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres
spring.datasource.username=postgres.hanmurmflpqbfwwvqnsl
spring.datasource.password=KuberFashion2025@
```

**If you want to use different database:**
```properties
spring.datasource.url=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### 2. JWT Secret (IMPORTANT for production)
**Current (OK for development):**
```properties
jwt.secret=kuberfashion-secret-key-2025-secure-token
```

**For production, change to:**
```properties
jwt.secret=your-super-secure-secret-key-minimum-32-characters-long
```

### 3. CORS (Frontend URL)
**Current:**
```properties
cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000
```

**Add your frontend URLs:**
```properties
cors.allowed-origins=http://localhost:3000,https://yourdomain.com
```

## 🧪 TEST EVERYTHING

### Test 1: Health Check
```bash
curl http://localhost:8080/api/test/health
```

### Test 2: Get Categories
```bash
curl http://localhost:8080/api/categories
```

### Test 3: Get Products
```bash
curl http://localhost:8080/api/products
```

### Test 4: Register User
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

### Test 5: Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@kuberfashion.com",
    "password": "test123"
  }'
```

## 🔐 PRE-LOADED TEST ACCOUNTS

### Admin Account:
- **Email:** `admin@kuberfashion.com`
- **Password:** `admin123`
- **Role:** ADMIN

### Regular User:
- **Email:** `test@kuberfashion.com`
- **Password:** `test123`
- **Role:** USER

## 📱 FRONTEND INTEGRATION

### API Base URL:
```javascript
const API_BASE_URL = 'http://localhost:8080/api';
```

### Login Example:
```javascript
const login = async (email, password) => {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  const result = await response.json();
  if (result.success) {
    localStorage.setItem('token', result.data.token);
    localStorage.setItem('user', JSON.stringify(result.data.user));
  }
  return result;
};
```

### Authenticated Requests:
```javascript
const token = localStorage.getItem('token');
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};

// Example: Get user wishlist
const getWishlist = async () => {
  const response = await fetch(`${API_BASE_URL}/wishlist`, {
    headers
  });
  return response.json();
};
```

## 🚨 COMMON ISSUES & SOLUTIONS

### Issue 1: "Port 8080 already in use"
**Solution:**
```bash
# Kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# OR change port in application.properties
server.port=8081
```

### Issue 2: "Database connection failed"
**Check:**
1. Is PostgreSQL running?
2. Are credentials correct in `application.properties`?
3. Is database accessible from your network?

**Test connection:**
```bash
# Test with psql (if installed)
psql -h aws-1-ap-south-1.pooler.supabase.com -p 5432 -U postgres.hanmurmflpqbfwwvqnsl -d postgres
```

### Issue 3: "Java not found"
**Solution:**
```bash
# Check Java installation
java -version

# If not found, install Java 17+ and add to PATH
```

### Issue 4: "Maven not found"
**Solution:**
```bash
# Check Maven installation
mvn -version

# If not found, install Maven and add to PATH
```

### Issue 5: "JWT token errors"
**Solution:**
- Ensure JWT secret is at least 32 characters
- Check `jwt.secret` in `application.properties`

## 📊 SAMPLE DATA INCLUDED

### Categories (6):
- Men's Fashion
- Women's Fashion  
- Footwear
- Accessories
- Kids Fashion
- Sports & Active

### Products (8):
- Classic Cotton T-Shirt ($29.99)
- Elegant Summer Dress ($79.99)
- Premium Sneakers ($129.99)
- Leather Crossbody Bag ($89.99)
- Kids Rainbow Hoodie ($39.99)
- Athletic Running Shorts ($34.99)
- Denim Jacket ($69.99)
- Silk Blouse ($95.99)

## 🎯 NEXT STEPS

1. ✅ **Run the backend** using `./run.bat`
2. ✅ **Test health endpoint** in browser
3. ✅ **Test API endpoints** using curl or Postman
4. ✅ **Connect your React frontend**
5. ✅ **Test authentication flow**
6. ✅ **Customize as needed**

## 📞 NEED HELP?

### Quick Debug Commands:
```bash
# Check if server is running
curl http://localhost:8080/api/test/health

# Check logs for errors
# (Look at console output when running mvn spring-boot:run)

# Test database connection
curl http://localhost:8080/api/test/database

# Run all tests
./test-api.bat
```

### Log Files:
- All logs appear in console
- Look for ERROR or WARN messages
- Database connection logs appear on startup

**Your backend is now ready! 🎉**
