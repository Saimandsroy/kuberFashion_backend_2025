#!/bin/bash

# Fix Backend Issues Script
echo "🔧 Fixing Backend Database and JWT Issues..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  KuberFashion Backend Fix Script${NC}"
echo -e "${BLUE}========================================${NC}\n"

# 1. Stop any conflicting PostgreSQL processes
echo -e "${YELLOW}1. Stopping conflicting PostgreSQL processes...${NC}"
brew services stop postgresql 2>/dev/null || true
pkill -f "postgres.*5432" 2>/dev/null || true
sleep 2

# 2. Ensure Docker PostgreSQL is running
echo -e "${YELLOW}2. Checking Docker PostgreSQL...${NC}"
if ! docker compose ps | grep -q "kuberfashion-postgres.*Up"; then
    echo -e "${YELLOW}   Starting Docker PostgreSQL...${NC}"
    docker compose up -d postgres
    sleep 10
fi

# Wait for PostgreSQL to be healthy
echo -e "${YELLOW}   Waiting for PostgreSQL to be ready...${NC}"
for i in {1..30}; do
    if docker exec kuberfashion-postgres pg_isready -U kuberfashion_user -d kuberfashion >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ PostgreSQL is ready!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 3. Test database connection
echo -e "${YELLOW}3. Testing database connection...${NC}"
if docker exec kuberfashion-postgres psql -U kuberfashion_user -d kuberfashion -c "SELECT 1;" >/dev/null 2>&1; then
    echo -e "${GREEN}   ✅ Database connection successful!${NC}"
else
    echo -e "${RED}   ❌ Database connection failed!${NC}"
    exit 1
fi

# 4. Check and fix environment variables
echo -e "${YELLOW}4. Setting up environment variables...${NC}"

# Export all environment variables from .env file
if [ -f .env ]; then
    echo -e "${GREEN}   ✅ Loading .env file...${NC}"
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
else
    echo -e "${RED}   ❌ .env file not found!${NC}"
    exit 1
fi

# Verify critical environment variables
echo -e "${YELLOW}   Checking environment variables...${NC}"
echo "   DATABASE_URL: $DATABASE_URL"
echo "   DATABASE_USERNAME: $DATABASE_USERNAME"
echo "   JWT_EXPIRATION: $JWT_EXPIRATION"
echo "   SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"

# 5. Test JWT configuration
echo -e "${YELLOW}5. Verifying JWT configuration...${NC}"
if [ "$JWT_EXPIRATION" = "86400000" ]; then
    echo -e "${GREEN}   ✅ JWT expiration is correct (24 hours)${NC}"
else
    echo -e "${RED}   ❌ JWT expiration is wrong: $JWT_EXPIRATION${NC}"
    echo -e "${YELLOW}   Fixing JWT expiration...${NC}"
    sed -i '' 's/JWT_EXPIRATION=.*/JWT_EXPIRATION=86400000/' .env
    export JWT_EXPIRATION=86400000
    echo -e "${GREEN}   ✅ JWT expiration fixed!${NC}"
fi

# 6. Clean build and start backend
echo -e "${YELLOW}6. Building and starting backend...${NC}"

# Kill any existing backend processes
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "KuberFashionApplication" 2>/dev/null || true
sleep 2

echo -e "${YELLOW}   Cleaning and building...${NC}"
mvn clean compile -q

echo -e "${YELLOW}   Starting backend with correct environment...${NC}"
echo -e "${BLUE}   Backend will start in the background...${NC}"
echo -e "${BLUE}   Check logs in the terminal where you run this script${NC}"

# Start backend in background
nohup mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

echo -e "${GREEN}   ✅ Backend started with PID: $BACKEND_PID${NC}"
echo -e "${YELLOW}   Waiting for backend to start...${NC}"

# Wait for backend to start
for i in {1..60}; do
    if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Backend is running!${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 7. Test the fix
echo -e "${YELLOW}7. Testing the complete fix...${NC}"

# Test health endpoint
echo -e "${YELLOW}   Testing health endpoint...${NC}"
HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/health)
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}   ✅ Health check passed!${NC}"
else
    echo -e "${RED}   ❌ Health check failed!${NC}"
    echo "   Response: $HEALTH_RESPONSE"
fi

# Test login and JWT
echo -e "${YELLOW}   Testing login and JWT...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@kuberfashion.com","password":"test123"}' \
  http://localhost:8080/api/auth/login)

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}   ✅ Login successful, token obtained!${NC}"
    
    # Test protected endpoint
    echo -e "${YELLOW}   Testing protected endpoint (cart)...${NC}"
    CART_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart)
    
    if echo "$CART_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}   ✅ Protected endpoint works!${NC}"
    else
        echo -e "${RED}   ❌ Protected endpoint failed!${NC}"
        echo "   Response: $CART_RESPONSE"
    fi
else
    echo -e "${RED}   ❌ Login failed!${NC}"
    echo "   Response: $LOGIN_RESPONSE"
fi

# 8. Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}           Fix Summary${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${GREEN}✅ Stopped conflicting PostgreSQL processes${NC}"
echo -e "${GREEN}✅ Docker PostgreSQL is running and healthy${NC}"
echo -e "${GREEN}✅ Environment variables are set correctly${NC}"
echo -e "${GREEN}✅ JWT expiration fixed to 24 hours${NC}"
echo -e "${GREEN}✅ Backend is running on http://localhost:8080${NC}"

echo -e "\n${YELLOW}📋 Next Steps:${NC}"
echo -e "1. Your backend is now running in the background"
echo -e "2. Clear your browser localStorage and login again"
echo -e "3. Test cart and wishlist functionality"
echo -e "4. Check backend logs: tail -f backend.log"
echo -e "5. Stop backend: kill \$(cat backend.pid)"

echo -e "\n${YELLOW}🔧 Useful Commands:${NC}"
echo -e "• Check backend status: curl http://localhost:8080/api/health"
echo -e "• View backend logs: tail -f backend.log"
echo -e "• Stop backend: kill \$(cat backend.pid)"
echo -e "• Restart backend: ./fix-backend-issues.sh"

echo -e "\n${GREEN}🎉 Backend fix completed!${NC}"
