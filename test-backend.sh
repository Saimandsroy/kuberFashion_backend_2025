#!/bin/bash

# KuberFashion Backend Automated Test Suite
# Usage: ./test-backend.sh [base_url]
# Example: ./test-backend.sh http://localhost:8080/api
# Example: ./test-backend.sh https://api.kuberfashions.in/api

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${1:-http://localhost:8080/api}"
TEST_USER_EMAIL="test_$(date +%s)@example.com"
TEST_USER_PASSWORD="TestPass123!"
TEST_USER_PHONE="+91$(date +%s | tail -c 11)"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@kuberfashion.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Temp file for responses
RESPONSE_FILE=$(mktemp)
trap "rm -f $RESPONSE_FILE" EXIT

# Helper functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}TEST: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    ((PASSED_TESTS++))
}

print_failure() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    echo -e "${RED}  Response: $(cat $RESPONSE_FILE | head -c 500)${NC}"
    ((FAILED_TESTS++))
}

run_test() {
    ((TOTAL_TESTS++))
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    local auth_token="$6"
    
    print_test "$test_name"
    
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"
    
    if [ -n "$auth_token" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $auth_token'"
    fi
    
    if [ -n "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    fi
    
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    local response=$(eval $curl_cmd)
    local status_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    
    echo "$body" > $RESPONSE_FILE
    
    if [ "$status_code" = "$expected_status" ]; then
        print_success "$test_name (Status: $status_code)"
        echo "$body"
        return 0
    else
        print_failure "$test_name (Expected: $expected_status, Got: $status_code)"
        return 1
    fi
}

extract_json_field() {
    local json="$1"
    local field="$2"
    echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | cut -d'"' -f4 | head -n1
}

# Start tests
print_header "KuberFashion Backend Test Suite"
echo "Base URL: $BASE_URL"
echo "Test User: $TEST_USER_EMAIL"
echo ""

# ==========================================
# 1. HEALTH CHECK
# ==========================================
print_header "1. Health Check"

run_test "Health endpoint" "GET" "/health" "" "200"

# ==========================================
# 2. PUBLIC ENDPOINTS
# ==========================================
print_header "2. Public Endpoints"

run_test "Get all categories" "GET" "/categories" "" "200"
run_test "Get all products" "GET" "/products" "" "200"
run_test "Get featured products" "GET" "/products/featured" "" "200"
run_test "Get newest products" "GET" "/products/newest" "" "200"
run_test "Search products" "GET" "/products/search?q=shirt" "" "200"

# ==========================================
# 3. USER REGISTRATION & AUTH
# ==========================================
print_header "3. User Registration & Authentication"

REGISTER_DATA="{
  \"firstName\": \"Test\",
  \"lastName\": \"User\",
  \"email\": \"$TEST_USER_EMAIL\",
  \"phone\": \"$TEST_USER_PHONE\",
  \"password\": \"$TEST_USER_PASSWORD\",
  \"confirmPassword\": \"$TEST_USER_PASSWORD\"
}"

REGISTER_RESPONSE=$(run_test "Register new user" "POST" "/auth/register" "$REGISTER_DATA" "201")
USER_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4 | head -n1)

if [ -z "$USER_TOKEN" ]; then
    echo -e "${RED}Failed to extract user token from registration${NC}"
    USER_TOKEN=""
fi

LOGIN_DATA="{
  \"email\": \"$TEST_USER_EMAIL\",
  \"password\": \"$TEST_USER_PASSWORD\"
}"

LOGIN_RESPONSE=$(run_test "Login user" "POST" "/auth/login" "$LOGIN_DATA" "200")
if [ -n "$LOGIN_RESPONSE" ]; then
    USER_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4 | head -n1)
fi

if [ -n "$USER_TOKEN" ]; then
    echo -e "${GREEN}User token obtained: ${USER_TOKEN:0:20}...${NC}"
    run_test "Get current user info" "GET" "/auth/me" "" "200" "$USER_TOKEN"
else
    echo -e "${RED}No user token available, skipping authenticated tests${NC}"
fi

# ==========================================
# 4. CART OPERATIONS (Authenticated)
# ==========================================
print_header "4. Cart Operations"

if [ -n "$USER_TOKEN" ]; then
    run_test "Get empty cart" "GET" "/cart" "" "200" "$USER_TOKEN"
    
    # Get a product ID first
    PRODUCTS_RESPONSE=$(curl -s "$BASE_URL/products?size=1")
    PRODUCT_ID=$(echo "$PRODUCTS_RESPONSE" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)
    
    if [ -n "$PRODUCT_ID" ]; then
        echo -e "${BLUE}Using product ID: $PRODUCT_ID${NC}"
        
        ADD_TO_CART_DATA="{
          \"productId\": $PRODUCT_ID,
          \"quantity\": 1,
          \"size\": null,
          \"color\": null
        }"
        
        CART_RESPONSE=$(run_test "Add item to cart" "POST" "/cart/add" "$ADD_TO_CART_DATA" "200" "$USER_TOKEN")
        CART_ITEM_ID=$(echo "$CART_RESPONSE" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)
        
        if [ -n "$CART_ITEM_ID" ]; then
            echo -e "${BLUE}Cart item ID: $CART_ITEM_ID${NC}"
            
            UPDATE_CART_DATA="{\"quantity\": 2}"
            run_test "Update cart item quantity" "PUT" "/cart/items/$CART_ITEM_ID" "$UPDATE_CART_DATA" "200" "$USER_TOKEN"
            
            run_test "Get cart with items" "GET" "/cart" "" "200" "$USER_TOKEN"
            run_test "Get cart count" "GET" "/cart/count" "" "200" "$USER_TOKEN"
            run_test "Remove item from cart" "DELETE" "/cart/items/$CART_ITEM_ID" "" "200" "$USER_TOKEN"
        fi
    else
        echo -e "${YELLOW}No products found, skipping cart item tests${NC}"
    fi
    
    run_test "Clear cart" "DELETE" "/cart" "" "200" "$USER_TOKEN"
else
    echo -e "${YELLOW}Skipping cart tests (no user token)${NC}"
fi

# ==========================================
# 5. WISHLIST OPERATIONS (Authenticated)
# ==========================================
print_header "5. Wishlist Operations"

if [ -n "$USER_TOKEN" ] && [ -n "$PRODUCT_ID" ]; then
    run_test "Get empty wishlist" "GET" "/wishlist" "" "200" "$USER_TOKEN"
    
    ADD_TO_WISHLIST_DATA="{\"productId\": $PRODUCT_ID}"
    run_test "Add item to wishlist" "POST" "/wishlist/add" "$ADD_TO_WISHLIST_DATA" "200" "$USER_TOKEN"
    run_test "Get wishlist with items" "GET" "/wishlist" "" "200" "$USER_TOKEN"
    run_test "Remove item from wishlist" "DELETE" "/wishlist/remove/$PRODUCT_ID" "" "200" "$USER_TOKEN"
else
    echo -e "${YELLOW}Skipping wishlist tests (no user token or product)${NC}"
fi

# ==========================================
# 6. ORDER OPERATIONS (Authenticated)
# ==========================================
print_header "6. Order Operations"

if [ -n "$USER_TOKEN" ] && [ -n "$PRODUCT_ID" ]; then
    # Add item to cart first
    ADD_TO_CART_DATA="{\"productId\": $PRODUCT_ID, \"quantity\": 1}"
    curl -s -X POST -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" \
         -d "$ADD_TO_CART_DATA" "$BASE_URL/cart/add" > /dev/null
    
    CREATE_ORDER_DATA="{
      \"cartItems\": [{\"id\": 1, \"productId\": $PRODUCT_ID, \"quantity\": 1, \"price\": 29.99}],
      \"shippingAddress\": \"123 Test Street, Test City, 12345\",
      \"billingAddress\": \"123 Test Street, Test City, 12345\",
      \"paymentMethod\": \"COD\"
    }"
    
    ORDER_RESPONSE=$(run_test "Create order" "POST" "/orders/create" "$CREATE_ORDER_DATA" "200" "$USER_TOKEN")
    ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"id":[0-9]*' | head -n1 | cut -d':' -f2)
    
    if [ -n "$ORDER_ID" ]; then
        echo -e "${BLUE}Order ID: $ORDER_ID${NC}"
        run_test "Get order by ID" "GET" "/orders/$ORDER_ID" "" "200" "$USER_TOKEN"
    fi
    
    run_test "Get user orders" "GET" "/orders/my-orders?page=0&size=10" "" "200" "$USER_TOKEN"
else
    echo -e "${YELLOW}Skipping order tests (no user token or product)${NC}"
fi

# ==========================================
# 7. ADMIN AUTHENTICATION
# ==========================================
print_header "7. Admin Authentication"

ADMIN_LOGIN_DATA="{
  \"email\": \"$ADMIN_EMAIL\",
  \"password\": \"$ADMIN_PASSWORD\"
}"

ADMIN_LOGIN_RESPONSE=$(run_test "Admin login" "POST" "/admin/auth/login" "$ADMIN_LOGIN_DATA" "200" || echo "")
ADMIN_TOKEN=$(echo "$ADMIN_LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4 | head -n1)

if [ -n "$ADMIN_TOKEN" ]; then
    echo -e "${GREEN}Admin token obtained: ${ADMIN_TOKEN:0:20}...${NC}"
else
    echo -e "${YELLOW}Admin login failed or no admin user exists. Skipping admin tests.${NC}"
    echo -e "${YELLOW}Create admin user with: POST /api/auth/create-admin?email=$ADMIN_EMAIL${NC}"
fi

# ==========================================
# 8. ADMIN OPERATIONS
# ==========================================
print_header "8. Admin Operations"

if [ -n "$ADMIN_TOKEN" ]; then
    run_test "Get all users (admin)" "GET" "/admin/users?page=0&size=10" "" "200" "$ADMIN_TOKEN"
    run_test "Get all orders (admin)" "GET" "/admin/orders?page=0&size=10" "" "200" "$ADMIN_TOKEN"
    run_test "Get all products (admin)" "GET" "/admin/products?page=0&size=10" "" "200" "$ADMIN_TOKEN"
    
    if [ -n "$ORDER_ID" ]; then
        UPDATE_ORDER_STATUS_DATA="{\"status\": \"PROCESSING\"}"
        run_test "Update order status (admin)" "PUT" "/admin/orders/$ORDER_ID/status" "$UPDATE_ORDER_STATUS_DATA" "200" "$ADMIN_TOKEN"
    fi
else
    echo -e "${YELLOW}Skipping admin tests (no admin token)${NC}"
fi

# ==========================================
# SUMMARY
# ==========================================
print_header "Test Summary"

echo -e "Total Tests:  ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:       ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:       ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}✓ All tests passed!${NC}\n"
    exit 0
else
    echo -e "\n${RED}✗ Some tests failed${NC}\n"
    exit 1
fi
