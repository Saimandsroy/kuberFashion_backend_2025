# KuberFashion Backend API

A comprehensive Spring Boot REST API for the KuberFashion e-commerce platform, providing secure authentication, product management, shopping cart, wishlist, and order processing capabilities.

## Features

- **User Authentication & Authorization**: JWT-based authentication with role-based access control
- **Product Management**: Complete product catalog with categories, filtering, and search
- **Shopping Cart**: Session-based cart management with persistence
- **Wishlist**: User wishlist functionality
- **Order Management**: Full order lifecycle from creation to delivery
- **Admin Panel**: Administrative endpoints for managing products, orders, and users
- **Security**: Comprehensive security with password encryption and JWT tokens
- **Database**: PostgreSQL with JPA/Hibernate ORM
- **Validation**: Input validation and error handling
- **CORS**: Configured for frontend integration

## Tech Stack

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security 6.x**
- **Spring Data JPA**
- **PostgreSQL**
- **JWT (JSON Web Tokens)**
- **Maven**
- **Hibernate**

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database (or Supabase)
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Quick Start

### 1. Clone and Setup

```bash
cd backend
```

### 2. Database Configuration

Update `src/main/resources/application.properties` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://your-db-host:5432/your-database
spring.datasource.username=your-username
spring.datasource.password=your-password
```

### 3. Build and Run

```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/auth/check-email/{email}` - Check email availability
- `GET /api/auth/check-phone/{phone}` - Check phone availability

### Products
- `GET /api/products` - Get all products (with pagination and filtering)
- `GET /api/products/{slug}` - Get product by slug
- `GET /api/products/category/{categorySlug}` - Get products by category
- `GET /api/products/featured` - Get featured products
- `GET /api/products/trending` - Get trending products
- `GET /api/products/search?q={query}` - Search products

### Categories
- `GET /api/categories` - Get all categories
- `GET /api/categories/{slug}` - Get category by slug
- `GET /api/categories/check-slug/{slug}` - Check slug availability

### Wishlist (Authenticated)
- `GET /api/wishlist` - Get user wishlist
- `POST /api/wishlist/add/{productId}` - Add product to wishlist
- `DELETE /api/wishlist/remove/{productId}` - Remove from wishlist
- `DELETE /api/wishlist/clear` - Clear wishlist
- `GET /api/wishlist/check/{productId}` - Check if product in wishlist

### Orders (Authenticated)
- `POST /api/orders/create` - Create new order
- `GET /api/orders/{orderId}` - Get order details
- `GET /api/orders/my-orders` - Get user orders
- `PUT /api/orders/{orderId}/cancel` - Cancel order

### User Profile (Authenticated)
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile
- `PUT /api/users/profile/password` - Change password
- `DELETE /api/users/profile` - Delete account

### Admin Endpoints (Admin Role Required)
- `GET /api/orders/admin/all` - Get all orders
- `PUT /api/orders/admin/{orderId}/status` - Update order status
- `GET /api/users/admin/{userId}` - Get user by ID
- `PUT /api/users/admin/{userId}/status` - Enable/disable user

## Authentication

The API uses JWT (JSON Web Tokens) for authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Sample Login Request

```json
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Sample Response

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "email": "user@example.com",
      "role": "USER"
    }
  }
}
```

## Sample Data

The application includes a data initialization service that creates:

- **6 Categories**: Men's Fashion, Women's Fashion, Footwear, Accessories, Kids Fashion, Sports & Active
- **8 Sample Products**: Various clothing items with different categories
- **2 Test Users**: 
  - Admin: `admin@kuberfashion.com` / `admin123`
  - User: `test@kuberfashion.com` / `test123`

## Configuration

### JWT Configuration
```properties
jwt.secret=kuberfashion-secret-key-2025-secure-token
jwt.expiration=86400000  # 24 hours
```

### CORS Configuration
```properties
cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000
```

## Error Handling

The API provides consistent error responses:

```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2025-01-11T10:30:00"
}
```

## Security Features

- Password encryption using BCrypt
- JWT token-based authentication
- Role-based access control (USER, ADMIN)
- CORS configuration for frontend integration
- Input validation and sanitization
- SQL injection prevention through JPA

## Database Schema

The application creates the following main tables:
- `users` - User accounts and authentication
- `categories` - Product categories
- `products` - Product catalog
- `orders` - Order information
- `order_items` - Order line items
- `wishlist_items` - User wishlist items

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
java -jar target/kuberfashion-backend-1.0.0.jar
```

## Frontend Integration

This backend is designed to work seamlessly with the KuberFashion React frontend. The API endpoints match the expected frontend service calls, and CORS is configured for `http://localhost:3000`.

---

## 🐳 Docker Deployment

### Prerequisites
- Docker Engine 20.10+
- Docker Compose v2.0+
- Git

### Quick Start with Docker

```bash
# Clone the repository
git clone <your-repo-url>
cd kuberFashion_backend_2025-main

# Create .env file from example
cp .env.example .env

# Edit .env with your production values
nano .env

# Start all services (Backend + Postgres + Redis)
docker-compose up -d --build

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down
```

### VPS Deployment Steps

#### 1. Install Docker on VPS (Ubuntu/Debian)

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sudo sh

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose plugin
sudo apt install docker-compose-plugin -y

# Verify installation
docker --version
docker compose version
```

#### 2. Clone and Configure

```bash
# Clone repository
git clone <your-repo-url>
cd kuberFashion_backend_2025-main

# Create production .env file
cp .env.example .env

# Edit with your production values
nano .env
```

**Required Environment Variables:**
```env
# Database
DATABASE_USERNAME=kuberfashion_user
DATABASE_PASSWORD=<strong-password>

# JWT Secret (generate a secure key)
JWT_SECRET=<your-256-bit-secret>

# CORS (your frontend URL)
CORS_ALLOWED_ORIGINS=https://yourdomain.com

# PhonePe (if using payments)
PHONEPE_CLIENT_ID=<your-client-id>
PHONEPE_CLIENT_SECRET=<your-secret>
PHONEPE_ENVIRONMENT=PRODUCTION
PHONEPE_REDIRECT_URL=https://yourdomain.com/payment/status

# Supabase (if using for auth)
SUPABASE_URL=<your-supabase-url>
SUPABASE_ANON_KEY=<your-anon-key>
```

#### 3. Deploy

```bash
# Build and start in background
docker compose up -d --build

# Check status
docker compose ps

# View logs
docker compose logs -f
```

#### 4. Setup Reverse Proxy (Nginx)

```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Database Backup & Restore

#### Backup Database
```bash
# Create backup
docker compose exec postgres pg_dump -U kuberfashion_user kuberfashion > backup_$(date +%Y%m%d).sql

# Or use pg_dumpall for full backup
docker compose exec postgres pg_dumpall -U kuberfashion_user > full_backup.sql
```

#### Restore Database
```bash
# Restore from backup
cat backup.sql | docker compose exec -T postgres psql -U kuberfashion_user -d kuberfashion
```

### Migration from Existing Database

If you have an existing Postgres database and want to migrate to Docker:

```bash
# 1. Export from source database
pg_dump -h source-host -U username -d kuberfashion > migration_backup.sql

# 2. Start Docker Postgres
docker compose up -d postgres

# 3. Wait for Postgres to be ready
docker compose exec postgres pg_isready -U kuberfashion_user

# 4. Import data
cat migration_backup.sql | docker compose exec -T postgres psql -U kuberfashion_user -d kuberfashion

# 5. Start backend
docker compose up -d backend
```

### Useful Docker Commands

```bash
# Rebuild without cache
docker compose build --no-cache

# View resource usage
docker stats

# Enter backend container
docker compose exec backend sh

# Enter database container
docker compose exec postgres psql -U kuberfashion_user -d kuberfashion

# Remove all data (WARNING: destructive)
docker compose down -v
```

### Docker Volumes

The following data is persisted across container restarts:
- `postgres_data` - PostgreSQL database files
- `redis_data` - Redis persistence
- `backend_uploads` - Uploaded files
- `backend_logs` - Application logs

---

## Support

For issues and questions, please check the API documentation or contact the development team.
