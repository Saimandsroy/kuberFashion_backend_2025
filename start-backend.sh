#!/bin/bash

# Simple Backend Startup Script
echo "🚀 Starting KuberFashion Backend..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Stop any existing backend processes
echo -e "${YELLOW}Stopping existing backend processes...${NC}"
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "KuberFashionApplication" 2>/dev/null || true
sleep 2

# Export all environment variables from .env file
echo -e "${YELLOW}Loading environment variables...${NC}"
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | grep -v '^export' | xargs)
    echo -e "${GREEN}✅ Environment variables loaded${NC}"
else
    echo -e "${RED}❌ .env file not found!${NC}"
    exit 1
fi

# Verify critical variables
echo -e "${YELLOW}Verifying environment variables...${NC}"
echo "DATABASE_URL: $DATABASE_URL"
echo "JWT_EXPIRATION: $JWT_EXPIRATION"
echo "CLOUDFLARE_R2_ACCESS_KEY: ${CLOUDFLARE_R2_ACCESS_KEY:0:10}..."
echo "SUPABASE_STORAGE_BUCKET: $SUPABASE_STORAGE_BUCKET"

# Check if PostgreSQL is running
echo -e "${YELLOW}Checking PostgreSQL...${NC}"
if ! docker exec kuberfashion-postgres pg_isready -U kuberfashion_user -d kuberfashion >/dev/null 2>&1; then
    echo -e "${RED}❌ PostgreSQL not ready. Starting it...${NC}"
    docker compose up -d postgres
    
    # Wait for PostgreSQL
    for i in {1..30}; do
        if docker exec kuberfashion-postgres pg_isready -U kuberfashion_user -d kuberfashion >/dev/null 2>&1; then
            echo -e "${GREEN}✅ PostgreSQL is ready!${NC}"
            break
        fi
        echo -n "."
        sleep 1
    done
else
    echo -e "${GREEN}✅ PostgreSQL is already running${NC}"
fi

# Clean build
echo -e "${YELLOW}Building application...${NC}"
mvn clean compile -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Build successful${NC}"
else
    echo -e "${RED}❌ Build failed${NC}"
    exit 1
fi

# Start backend
echo -e "${YELLOW}Starting backend...${NC}"
echo -e "${BLUE}Backend will start in the foreground. Press Ctrl+C to stop.${NC}"
echo -e "${BLUE}To run in background, use: nohup ./start-backend.sh > backend.log 2>&1 &${NC}"
echo ""

# Start with all environment variables
mvn spring-boot:run
