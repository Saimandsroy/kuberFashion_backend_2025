#!/bin/bash

# Quick Backend Test Script
echo "🧪 Testing Backend Endpoints..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api"

# Test health endpoint
echo -e "${YELLOW}Testing health endpoint...${NC}"
HEALTH_RESPONSE=$(curl -s $BASE_URL/health)
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Health check passed${NC}"
    echo "$HEALTH_RESPONSE" | jq '.' 2>/dev/null || echo "$HEALTH_RESPONSE"
else
    echo -e "${RED}❌ Health check failed${NC}"
    echo "$HEALTH_RESPONSE"
    exit 1
fi

# Test products endpoint
echo -e "\n${YELLOW}Testing products endpoint...${NC}"
PRODUCTS_RESPONSE=$(curl -s $BASE_URL/products)
if echo "$PRODUCTS_RESPONSE" | grep -q '"success":true'; then
    PRODUCT_COUNT=$(echo "$PRODUCTS_RESPONSE" | grep -o '"id":[0-9]*' | wc -l)
    echo -e "${GREEN}✅ Products endpoint working - $PRODUCT_COUNT products found${NC}"
else
    echo -e "${RED}❌ Products endpoint failed${NC}"
    echo "$PRODUCTS_RESPONSE" | head -c 200
fi

# Test categories endpoint
echo -e "\n${YELLOW}Testing categories endpoint...${NC}"
CATEGORIES_RESPONSE=$(curl -s $BASE_URL/categories)
if echo "$CATEGORIES_RESPONSE" | grep -q '"success":true'; then
    CATEGORY_COUNT=$(echo "$CATEGORIES_RESPONSE" | grep -o '"id":[0-9]*' | wc -l)
    echo -e "${GREEN}✅ Categories endpoint working - $CATEGORY_COUNT categories found${NC}"
else
    echo -e "${RED}❌ Categories endpoint failed${NC}"
    echo "$CATEGORIES_RESPONSE" | head -c 200
fi

# Test login
echo -e "\n${YELLOW}Testing login endpoint...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@kuberfashion.com","password":"test123"}' \
  $BASE_URL/auth/login)

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}✅ Login successful - Token obtained${NC}"
    echo "Token: ${TOKEN:0:20}..."
    
    # Test protected endpoint
    echo -e "\n${YELLOW}Testing protected endpoint (cart)...${NC}"
    CART_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" $BASE_URL/cart)
    if echo "$CART_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}✅ Protected endpoint working - Cart accessible${NC}"
    else
        echo -e "${RED}❌ Protected endpoint failed${NC}"
        echo "$CART_RESPONSE" | head -c 200
    fi
    
    # Test wishlist
    echo -e "\n${YELLOW}Testing wishlist endpoint...${NC}"
    WISHLIST_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" $BASE_URL/wishlist)
    if echo "$WISHLIST_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}✅ Wishlist endpoint working${NC}"
    else
        echo -e "${RED}❌ Wishlist endpoint failed${NC}"
        echo "$WISHLIST_RESPONSE" | head -c 200
    fi
else
    echo -e "${RED}❌ Login failed${NC}"
    echo "$LOGIN_RESPONSE" | head -c 200
fi

echo -e "\n${GREEN}🎉 Backend testing completed!${NC}"
