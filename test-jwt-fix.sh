#!/bin/bash

# Test JWT Token Fix
echo "🔧 Testing JWT Token Fix..."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api"

echo -e "${YELLOW}1. Testing login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@kuberfashion.com","password":"test123"}' \
  "$BASE_URL/auth/login")

echo "Login response: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Failed to get token from login${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Token obtained: ${TOKEN:0:20}...${NC}"

# Decode JWT payload to check expiration
echo -e "${YELLOW}2. Checking token expiration...${NC}"
PAYLOAD=$(echo "$TOKEN" | cut -d'.' -f2)
# Add padding if needed
case $((${#PAYLOAD} % 4)) in
    2) PAYLOAD="${PAYLOAD}==" ;;
    3) PAYLOAD="${PAYLOAD}=" ;;
esac

DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null)
echo "Decoded payload: $DECODED"

IAT=$(echo "$DECODED" | grep -o '"iat":[0-9]*' | cut -d':' -f2)
EXP=$(echo "$DECODED" | grep -o '"exp":[0-9]*' | cut -d':' -f2)

if [ -n "$IAT" ] && [ -n "$EXP" ]; then
    DURATION=$((EXP - IAT))
    echo -e "${GREEN}✅ Token duration: $DURATION seconds ($(($DURATION / 3600)) hours)${NC}"
    
    if [ $DURATION -lt 3600 ]; then
        echo -e "${RED}❌ Token expires too quickly! Duration: $DURATION seconds${NC}"
    else
        echo -e "${GREEN}✅ Token duration looks good!${NC}"
    fi
else
    echo -e "${RED}❌ Could not parse token timestamps${NC}"
fi

echo -e "${YELLOW}3. Testing protected endpoint (cart)...${NC}"
CART_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/cart")

if echo "$CART_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Cart endpoint works with token!${NC}"
    echo "Cart response: $CART_RESPONSE"
else
    echo -e "${RED}❌ Cart endpoint failed${NC}"
    echo "Cart response: $CART_RESPONSE"
fi

echo -e "${YELLOW}4. Testing wishlist endpoint...${NC}"
WISHLIST_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/wishlist")

if echo "$WISHLIST_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Wishlist endpoint works with token!${NC}"
    echo "Wishlist response: $WISHLIST_RESPONSE"
else
    echo -e "${RED}❌ Wishlist endpoint failed${NC}"
    echo "Wishlist response: $WISHLIST_RESPONSE"
fi

echo -e "${YELLOW}5. Testing add to cart...${NC}"
ADD_CART_RESPONSE=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 1, "size": null, "color": null}' \
  "$BASE_URL/cart/add")

if echo "$ADD_CART_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Add to cart works!${NC}"
    echo "Add to cart response: $ADD_CART_RESPONSE"
else
    echo -e "${RED}❌ Add to cart failed${NC}"
    echo "Add to cart response: $ADD_CART_RESPONSE"
fi

echo -e "${YELLOW}✅ JWT fix test completed!${NC}"
