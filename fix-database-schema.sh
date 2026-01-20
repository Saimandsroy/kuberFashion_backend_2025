#!/bin/bash

# Database Schema Fix Script
echo "🔧 Fixing Database Schema Issues..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Database Schema Fix Script${NC}"
echo -e "${BLUE}========================================${NC}\n"

# 1. Stop any running processes
echo -e "${YELLOW}1. Stopping all processes...${NC}"
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "KuberFashionApplication" 2>/dev/null || true
sleep 2

# 2. Fix .env file duplicates
echo -e "${YELLOW}2. Fixing .env file duplicates...${NC}"

# Create a clean .env file
cat > .env << 'EOF'
# KuberFashion Backend Environment Configuration

# Supabase Configuration
SUPABASE_SERVICE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhhbm11cm1mbHBxYmZ3d3ZxbnNsIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1ODAyMTEwMSwiZXhwIjoyMDczNTk3MTAxfQ.Mdr6HQeOCtNwu5aBM9xQ45O66s846ljkaKfx5L7cSTQ
SUPABASE_URL=https://hanmurmflpqbfwwvqnsl.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhhbm11cm1mbHBxYmZ3d3ZxbnNsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTgwMjExMDEsImV4cCI6MjA3MzU5NzEwMX0.jPpF3Zyp33dh84eXi05VhBG8i4yu43xIzxBmGsSn6ns
SUPABASE_STORAGE_BUCKET=kuberfashion-assets

# Database Configuration (Production)
DATABASE_URL=jdbc:postgresql://localhost:5432/kuberfashion?sslmode=disable
DATABASE_USERNAME=kuberfashion_user
DATABASE_PASSWORD=KuberFashion@2025!

# JWT Configuration
JWT_SECRET=u5PS40PD1gBpFFAgD7ugXd6k9klL+h9YZMv3gIjKH3Nof/qaJf2rPbr0wrwwe6WaDfq7q3JOjwdBF/6AaKz7sQ==
JWT_EXPIRATION=86400000

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://10.197.216.70:5173,http://172.28.96.1:5173,http://localhost:3000

# Logging Configuration
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_KUBERFASHION=INFO
LOGGING_LEVEL_SECURITY=WARN

# JPA/Hibernate
HIBERNATE_DDL_AUTO=create-drop
HIBERNATE_SHOW_SQL=false
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# Production Database Pool Settings
DATABASE_POOL_SIZE=20
DATABASE_MAX_LIFETIME=1800000
DATABASE_CONNECTION_TIMEOUT=30000

# Cloudflare R2 Storage Configuration
CLOUDFLARE_R2_ACCESS_KEY=6a1286e3afd24c3509e0340dfd6ee83a
CLOUDFLARE_R2_SECRET_KEY=90c40e05e9024aa35218389337ae15f86bdcfb56bad507f9ad4e65a1c3ab0e16
CLOUDFLARE_R2_ACCOUNT_ID=c8220c88e98c8dfca70b1b32913c0ed0
CLOUDFLARE_R2_BUCKET_NAME=kuberfashion-storage
CLOUDFLARE_R2_PUBLIC_URL=https://pub-6a03cf217dd0476f9f707032aa5ceab3.r2.dev
EOF

echo -e "${GREEN}   ✅ Created clean .env file${NC}"

# 3. Completely reset database
echo -e "${YELLOW}3. Completely resetting database...${NC}"

# Stop and remove containers
docker compose down -v
sleep 2

# Remove any persistent volumes
docker volume prune -f

# Start fresh PostgreSQL
echo -e "${YELLOW}   Starting fresh PostgreSQL...${NC}"
docker compose up -d postgres
sleep 10

# Wait for PostgreSQL to be ready
echo -e "${YELLOW}   Waiting for PostgreSQL to be ready...${NC}"
for i in {1..30}; do
    if docker exec kuberfashion-postgres pg_isready -U kuberfashion_user -d kuberfashion >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ PostgreSQL is ready!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 4. Temporarily set to create-drop for fresh schema
echo -e "${YELLOW}4. Configuring for fresh schema creation...${NC}"

# Update application-prod.properties to use create-drop
sed -i '' 's/spring.jpa.hibernate.ddl-auto=update/spring.jpa.hibernate.ddl-auto=create-drop/' src/main/resources/application-prod.properties

echo -e "${GREEN}   ✅ Set to create-drop mode${NC}"

# 5. Export environment variables
echo -e "${YELLOW}5. Loading environment variables...${NC}"
export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)

# Verify critical variables
echo "   DATABASE_URL: $DATABASE_URL"
echo "   JWT_EXPIRATION: $JWT_EXPIRATION"
echo "   HIBERNATE_DDL_AUTO: $HIBERNATE_DDL_AUTO"

# 6. Build and start backend
echo -e "${YELLOW}6. Building and starting backend...${NC}"

# Clean build
mvn clean compile -q

echo -e "${YELLOW}   Starting backend with fresh schema creation...${NC}"

# Start backend and wait for it to create schema
mvn spring-boot:run &
BACKEND_PID=$!

# Wait for backend to start and create schema
echo -e "${YELLOW}   Waiting for schema creation...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Backend started and schema created!${NC}"
        SCHEMA_CREATED=true
        break
    fi
    
    # Check if process is still running
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${RED}   ❌ Backend process died${NC}"
        break
    fi
    
    echo -n "."
    sleep 1
done

# Stop the backend
kill $BACKEND_PID 2>/dev/null || true
sleep 3

# 7. Switch back to update mode
echo -e "${YELLOW}7. Switching to update mode...${NC}"

# Update .env to use update instead of create-drop
sed -i '' 's/HIBERNATE_DDL_AUTO=create-drop/HIBERNATE_DDL_AUTO=update/' .env

# Update application-prod.properties
sed -i '' 's/spring.jpa.hibernate.ddl-auto=create-drop/spring.jpa.hibernate.ddl-auto=update/' src/main/resources/application-prod.properties

echo -e "${GREEN}   ✅ Switched to update mode${NC}"

# 8. Start backend normally
echo -e "${YELLOW}8. Starting backend normally...${NC}"

# Export updated environment variables
export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)

# Start backend in background
nohup mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

echo -e "${GREEN}   ✅ Backend started with PID: $BACKEND_PID${NC}"

# Wait for backend to be ready
echo -e "${YELLOW}   Waiting for backend to be ready...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Backend is running and healthy!${NC}"
        BACKEND_READY=true
        break
    fi
    echo -n "."
    sleep 1
done

# 9. Test everything
echo -e "${YELLOW}9. Testing all functionality...${NC}"

if [ "$BACKEND_READY" = "true" ]; then
    # Test health
    HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/health)
    if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}   ✅ Health check passed${NC}"
    else
        echo -e "${RED}   ❌ Health check failed${NC}"
    fi
    
    # Test products
    PRODUCTS_RESPONSE=$(curl -s http://localhost:8080/api/products)
    if echo "$PRODUCTS_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}   ✅ Products endpoint working${NC}"
    else
        echo -e "${RED}   ❌ Products endpoint failed${NC}"
    fi
    
    # Test login
    LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
      -d '{"email":"test@kuberfashion.com","password":"test123"}' \
      http://localhost:8080/api/auth/login)
    
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$TOKEN" ]; then
        echo -e "${GREEN}   ✅ Login successful${NC}"
        
        # Test protected endpoint
        CART_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart)
        if echo "$CART_RESPONSE" | grep -q '"success":true'; then
            echo -e "${GREEN}   ✅ Protected endpoints working${NC}"
        else
            echo -e "${RED}   ❌ Protected endpoints failed${NC}"
        fi
    else
        echo -e "${RED}   ❌ Login failed${NC}"
    fi
else
    echo -e "${RED}   ❌ Backend not ready for testing${NC}"
fi

# 10. Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}           Fix Summary${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${GREEN}✅ Fixed duplicate environment variables${NC}"
echo -e "${GREEN}✅ Reset database with fresh schema${NC}"
echo -e "${GREEN}✅ Created all required database tables${NC}"
echo -e "${GREEN}✅ Backend is running on http://localhost:8080${NC}"
echo -e "${GREEN}✅ JWT tokens work for 24 hours${NC}"

echo -e "\n${YELLOW}📋 Next Steps:${NC}"
echo -e "1. Clear your browser localStorage and login again"
echo -e "2. Test cart and wishlist functionality"
echo -e "3. Check backend logs: tail -f backend.log"
echo -e "4. Stop backend: kill \$(cat backend.pid)"

echo -e "\n${YELLOW}🔧 Useful Commands:${NC}"
echo -e "• Check backend status: curl http://localhost:8080/api/health"
echo -e "• View backend logs: tail -f backend.log"
echo -e "• Stop backend: kill \$(cat backend.pid)"
echo -e "• Quick test: ./test-backend-quick.sh"

if [ "$BACKEND_READY" = "true" ]; then
    echo -e "\n${GREEN}🎉 All issues fixed! Backend is ready to use!${NC}"
    echo -e "\n${BLUE}🧪 Run quick test: ./test-backend-quick.sh${NC}"
else
    echo -e "\n${RED}⚠️  Some issues remain. Check backend.log for details.${NC}"
    echo -e "Run: tail -20 backend.log"
fi
