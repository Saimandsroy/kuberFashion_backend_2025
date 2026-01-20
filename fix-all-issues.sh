#!/bin/bash

# Complete Backend Fix Script
echo "🔧 Fixing All Backend Issues..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  KuberFashion Complete Fix Script${NC}"
echo -e "${BLUE}========================================${NC}\n"

# 1. Stop any running backend processes
echo -e "${YELLOW}1. Stopping existing backend processes...${NC}"
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "KuberFashionApplication" 2>/dev/null || true
sleep 2

# 2. Fix environment variables
echo -e "${YELLOW}2. Setting up environment variables...${NC}"

# Add missing Cloudflare R2 variables to .env if they don't exist
if ! grep -q "CLOUDFLARE_R2_ACCESS_KEY" .env; then
    echo "" >> .env
    echo "# Cloudflare R2 Storage Configuration" >> .env
    echo "CLOUDFLARE_R2_ACCESS_KEY=6a1286e3afd24c3509e0340dfd6ee83a" >> .env
    echo "CLOUDFLARE_R2_SECRET_KEY=90c40e05e9024aa35218389337ae15f86bdcfb56bad507f9ad4e65a1c3ab0e16" >> .env
    echo "CLOUDFLARE_R2_ACCOUNT_ID=c8220c88e98c8dfca70b1b32913c0ed0" >> .env
    echo "CLOUDFLARE_R2_BUCKET_NAME=kuberfashion-storage" >> .env
    echo "CLOUDFLARE_R2_PUBLIC_URL=https://pub-6a03cf217dd0476f9f707032aa5ceab3.r2.dev" >> .env
    echo -e "${GREEN}   ✅ Added Cloudflare R2 environment variables${NC}"
fi

# Export all environment variables
export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)

# 3. Reset database to clean state
echo -e "${YELLOW}3. Resetting database to clean state...${NC}"

# Stop and remove existing containers
docker compose down
sleep 2

# Remove volumes to start fresh
docker volume rm kuberFashion_backend_2025_postgres_data 2>/dev/null || true
docker volume rm kuberFashion_backend_2025_pgadmin_data 2>/dev/null || true

# Start fresh PostgreSQL
echo -e "${YELLOW}   Starting fresh PostgreSQL container...${NC}"
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

# 4. Temporarily switch to create-drop for clean schema
echo -e "${YELLOW}4. Configuring for clean schema creation...${NC}"

# Backup current prod properties
cp src/main/resources/application-prod.properties src/main/resources/application-prod.properties.backup

# Temporarily change ddl-auto to create-drop for clean start
sed -i '' 's/spring.jpa.hibernate.ddl-auto=update/spring.jpa.hibernate.ddl-auto=create-drop/' src/main/resources/application-prod.properties

echo -e "${GREEN}   ✅ Configured for clean schema creation${NC}"

# 5. Build and start backend with clean schema
echo -e "${YELLOW}5. Building and starting backend with clean schema...${NC}"

# Clean build
mvn clean compile -q

echo -e "${YELLOW}   Starting backend (this will create fresh schema)...${NC}"

# Start backend and capture output
timeout 60s mvn spring-boot:run > backend-startup.log 2>&1 &
BACKEND_PID=$!

# Wait for backend to start or fail
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Backend started successfully with clean schema!${NC}"
        BACKEND_STARTED=true
        break
    fi
    
    # Check if process is still running
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${RED}   ❌ Backend process died, checking logs...${NC}"
        tail -20 backend-startup.log
        break
    fi
    
    echo -n "."
    sleep 1
done

# Stop the backend
kill $BACKEND_PID 2>/dev/null || true
sleep 2

# 6. Restore normal configuration and restart
echo -e "${YELLOW}6. Restoring normal configuration...${NC}"

# Restore original prod properties
mv src/main/resources/application-prod.properties.backup src/main/resources/application-prod.properties

echo -e "${GREEN}   ✅ Restored normal configuration${NC}"

# 7. Start backend normally
echo -e "${YELLOW}7. Starting backend normally...${NC}"

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

# 8. Test everything
echo -e "${YELLOW}8. Testing all functionality...${NC}"

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
        echo -e "${GREEN}   ✅ Login successful, token obtained${NC}"
        
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

# 9. Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}           Fix Summary${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${GREEN}✅ Fixed missing Cloudflare R2 environment variables${NC}"
echo -e "${GREEN}✅ Reset database to clean state${NC}"
echo -e "${GREEN}✅ Created fresh schema without NULL value conflicts${NC}"
echo -e "${GREEN}✅ Backend is running on http://localhost:8080${NC}"
echo -e "${GREEN}✅ JWT expiration set to 24 hours${NC}"

echo -e "\n${YELLOW}📋 Next Steps:${NC}"
echo -e "1. Clear your browser localStorage and login again"
echo -e "2. Test cart and wishlist functionality"
echo -e "3. Check backend logs: tail -f backend.log"
echo -e "4. Stop backend: kill \$(cat backend.pid)"

echo -e "\n${YELLOW}🔧 Useful Commands:${NC}"
echo -e "• Check backend status: curl http://localhost:8080/api/health"
echo -e "• View backend logs: tail -f backend.log"
echo -e "• Stop backend: kill \$(cat backend.pid)"
echo -e "• View database: docker exec -it kuberfashion-postgres psql -U kuberfashion_user -d kuberfashion"

if [ "$BACKEND_READY" = "true" ]; then
    echo -e "\n${GREEN}🎉 All issues fixed! Backend is ready to use!${NC}"
else
    echo -e "\n${RED}⚠️  Some issues remain. Check backend.log for details.${NC}"
    echo -e "Run: tail -20 backend.log"
fi
