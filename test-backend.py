#!/usr/bin/env python3
"""
KuberFashion Backend Automated Test Suite (Python)
Usage: python3 test-backend.py [base_url]
Example: python3 test-backend.py http://localhost:8080/api
Example: python3 test-backend.py https://api.kuberfashions.in/api
"""

import sys
import json
import time
import requests
from datetime import datetime
from typing import Optional, Dict, Any

# Configuration
BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/api"
TEST_USER_EMAIL = f"test_{int(time.time())}@example.com"
TEST_USER_PASSWORD = "TestPass123!"
TEST_USER_PHONE = f"+91{int(time.time()) % 10000000000}"
ADMIN_EMAIL = "admin@kuberfashion.com"
ADMIN_PASSWORD = "admin123"

# Colors
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    CYAN = '\033[0;36m'
    NC = '\033[0m'

# Test counters
total_tests = 0
passed_tests = 0
failed_tests = 0

# Tokens
user_token = None
admin_token = None
product_id = None
cart_item_id = None
order_id = None

def print_header(text: str):
    print(f"\n{Colors.BLUE}{'=' * 60}{Colors.NC}")
    print(f"{Colors.BLUE}{text.center(60)}{Colors.NC}")
    print(f"{Colors.BLUE}{'=' * 60}{Colors.NC}\n")

def print_test(text: str):
    print(f"{Colors.YELLOW}TEST: {text}{Colors.NC}")

def print_success(text: str):
    global passed_tests
    print(f"{Colors.GREEN}✓ PASS: {text}{Colors.NC}")
    passed_tests += 1

def print_failure(text: str, details: str = ""):
    global failed_tests
    print(f"{Colors.RED}✗ FAIL: {text}{Colors.NC}")
    if details:
        print(f"{Colors.RED}  Details: {details}{Colors.NC}")
    failed_tests += 1

def print_info(text: str):
    print(f"{Colors.CYAN}ℹ INFO: {text}{Colors.NC}")

def run_test(
    name: str,
    method: str,
    endpoint: str,
    expected_status: int,
    data: Optional[Dict[Any, Any]] = None,
    token: Optional[str] = None,
    params: Optional[Dict[str, Any]] = None
) -> Optional[Dict[Any, Any]]:
    """Run a single test and return response JSON if successful"""
    global total_tests
    total_tests += 1
    
    print_test(name)
    
    url = f"{BASE_URL}{endpoint}"
    headers = {"Content-Type": "application/json"}
    
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    try:
        if method == "GET":
            response = requests.get(url, headers=headers, params=params, timeout=10)
        elif method == "POST":
            response = requests.post(url, headers=headers, json=data, timeout=10)
        elif method == "PUT":
            response = requests.put(url, headers=headers, json=data, timeout=10)
        elif method == "DELETE":
            response = requests.delete(url, headers=headers, timeout=10)
        else:
            print_failure(name, f"Unknown method: {method}")
            return None
        
        if response.status_code == expected_status:
            print_success(f"{name} (Status: {response.status_code})")
            try:
                return response.json()
            except:
                return {"status": "ok"}
        else:
            print_failure(
                name,
                f"Expected status {expected_status}, got {response.status_code}. "
                f"Response: {response.text[:200]}"
            )
            return None
            
    except requests.exceptions.ConnectionError:
        print_failure(name, f"Connection refused. Is the server running at {BASE_URL}?")
        return None
    except requests.exceptions.Timeout:
        print_failure(name, "Request timeout")
        return None
    except Exception as e:
        print_failure(name, f"Exception: {str(e)}")
        return None

def main():
    global user_token, admin_token, product_id, cart_item_id, order_id
    
    print_header("KuberFashion Backend Test Suite")
    print(f"Base URL: {BASE_URL}")
    print(f"Test User: {TEST_USER_EMAIL}")
    print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    
    # ==========================================
    # 1. HEALTH CHECK
    # ==========================================
    print_header("1. Health Check")
    
    health = run_test("Health endpoint", "GET", "/health", 200)
    if health:
        print_info(f"Service: {health.get('service', 'N/A')}, Status: {health.get('status', 'N/A')}")
    
    # ==========================================
    # 2. PUBLIC ENDPOINTS
    # ==========================================
    print_header("2. Public Endpoints")
    
    categories = run_test("Get all categories", "GET", "/categories", 200)
    if categories and isinstance(categories.get('data'), list):
        print_info(f"Found {len(categories['data'])} categories")
    
    products = run_test("Get all products", "GET", "/products", 200)
    if products and products.get('data'):
        content = products['data'].get('content', [])
        if content:
            product_id = content[0].get('id')
            print_info(f"Found {len(content)} products, using product ID: {product_id}")
    
    run_test("Get featured products", "GET", "/products/featured", 200)
    run_test("Get newest products", "GET", "/products/newest", 200)
    run_test("Search products", "GET", "/products/search", 200, params={"q": "shirt"})
    
    # ==========================================
    # 3. USER REGISTRATION & AUTH
    # ==========================================
    print_header("3. User Registration & Authentication")
    
    register_data = {
        "firstName": "Test",
        "lastName": "User",
        "email": TEST_USER_EMAIL,
        "phone": TEST_USER_PHONE,
        "password": TEST_USER_PASSWORD,
        "confirmPassword": TEST_USER_PASSWORD
    }
    
    register_response = run_test("Register new user", "POST", "/auth/register", 201, data=register_data)
    if register_response and register_response.get('data'):
        user_token = register_response['data'].get('token')
        if user_token:
            print_info(f"User token obtained: {user_token[:20]}...")
    
    login_data = {
        "email": TEST_USER_EMAIL,
        "password": TEST_USER_PASSWORD
    }
    
    login_response = run_test("Login user", "POST", "/auth/login", 200, data=login_data)
    if login_response and login_response.get('data'):
        user_token = login_response['data'].get('token')
        if user_token:
            print_info(f"User token refreshed: {user_token[:20]}...")
    
    if user_token:
        me_response = run_test("Get current user info", "GET", "/auth/me", 200, token=user_token)
        if me_response and me_response.get('data'):
            user_data = me_response['data']
            print_info(f"Logged in as: {user_data.get('firstName')} {user_data.get('lastName')}")
    else:
        print_info("No user token available, skipping authenticated tests")
    
    # ==========================================
    # 4. CART OPERATIONS
    # ==========================================
    print_header("4. Cart Operations")
    
    if user_token:
        run_test("Get empty cart", "GET", "/cart", 200, token=user_token)
        
        if product_id:
            add_cart_data = {
                "productId": product_id,
                "quantity": 1,
                "size": None,
                "color": None
            }
            
            cart_response = run_test("Add item to cart", "POST", "/cart/add", 200, data=add_cart_data, token=user_token)
            if cart_response and cart_response.get('data'):
                cart_item_id = cart_response['data'].get('id')
                print_info(f"Cart item ID: {cart_item_id}")
            
            if cart_item_id:
                update_data = {"quantity": 2}
                run_test("Update cart item quantity", "PUT", f"/cart/items/{cart_item_id}", 200, data=update_data, token=user_token)
                
                cart_items = run_test("Get cart with items", "GET", "/cart", 200, token=user_token)
                if cart_items and cart_items.get('data'):
                    print_info(f"Cart has {len(cart_items['data'])} items")
                
                run_test("Get cart count", "GET", "/cart/count", 200, token=user_token)
                run_test("Remove item from cart", "DELETE", f"/cart/items/{cart_item_id}", 200, token=user_token)
        else:
            print_info("No product ID available, skipping cart item tests")
        
        run_test("Clear cart", "DELETE", "/cart", 200, token=user_token)
    else:
        print_info("Skipping cart tests (no user token)")
    
    # ==========================================
    # 5. WISHLIST OPERATIONS
    # ==========================================
    print_header("5. Wishlist Operations")
    
    if user_token and product_id:
        run_test("Get empty wishlist", "GET", "/wishlist", 200, token=user_token)
        
        wishlist_data = {"productId": product_id}
        run_test("Add item to wishlist", "POST", "/wishlist/add", 200, data=wishlist_data, token=user_token)
        
        wishlist_items = run_test("Get wishlist with items", "GET", "/wishlist", 200, token=user_token)
        if wishlist_items and wishlist_items.get('data'):
            print_info(f"Wishlist has {len(wishlist_items['data'])} items")
        
        run_test("Remove item from wishlist", "DELETE", f"/wishlist/remove/{product_id}", 200, token=user_token)
    else:
        print_info("Skipping wishlist tests (no user token or product)")
    
    # ==========================================
    # 6. ORDER OPERATIONS
    # ==========================================
    print_header("6. Order Operations")
    
    if user_token and product_id:
        # Add item to cart first
        add_cart_data = {"productId": product_id, "quantity": 1}
        requests.post(
            f"{BASE_URL}/cart/add",
            headers={"Authorization": f"Bearer {user_token}", "Content-Type": "application/json"},
            json=add_cart_data
        )
        
        order_data = {
            "cartItems": [{"id": 1, "productId": product_id, "quantity": 1, "price": 29.99}],
            "shippingAddress": "123 Test Street, Test City, 12345",
            "billingAddress": "123 Test Street, Test City, 12345",
            "paymentMethod": "COD"
        }
        
        order_response = run_test("Create order", "POST", "/orders/create", 200, data=order_data, token=user_token)
        if order_response and order_response.get('data'):
            order_id = order_response['data'].get('id')
            print_info(f"Order ID: {order_id}")
        
        if order_id:
            run_test("Get order by ID", "GET", f"/orders/{order_id}", 200, token=user_token)
        
        orders = run_test("Get user orders", "GET", "/orders/my-orders", 200, params={"page": 0, "size": 10}, token=user_token)
        if orders and orders.get('data'):
            print_info(f"User has {orders['data'].get('totalElements', 0)} orders")
    else:
        print_info("Skipping order tests (no user token or product)")
    
    # ==========================================
    # 7. ADMIN AUTHENTICATION
    # ==========================================
    print_header("7. Admin Authentication")
    
    admin_login_data = {
        "email": ADMIN_EMAIL,
        "password": ADMIN_PASSWORD
    }
    
    admin_response = run_test("Admin login", "POST", "/admin/auth/login", 200, data=admin_login_data)
    if admin_response and admin_response.get('data'):
        admin_token = admin_response['data'].get('token')
        if admin_token:
            print_info(f"Admin token obtained: {admin_token[:20]}...")
    else:
        print_info(f"Admin login failed. Create admin with: POST /api/auth/create-admin?email={ADMIN_EMAIL}")
    
    # ==========================================
    # 8. ADMIN OPERATIONS
    # ==========================================
    print_header("8. Admin Operations")
    
    if admin_token:
        users = run_test("Get all users (admin)", "GET", "/admin/users", 200, params={"page": 0, "size": 10}, token=admin_token)
        if users and users.get('data'):
            print_info(f"Total users: {users['data'].get('totalElements', 0)}")
        
        orders = run_test("Get all orders (admin)", "GET", "/admin/orders", 200, params={"page": 0, "size": 10}, token=admin_token)
        if orders and orders.get('data'):
            print_info(f"Total orders: {orders['data'].get('totalElements', 0)}")
        
        products = run_test("Get all products (admin)", "GET", "/admin/products", 200, params={"page": 0, "size": 10}, token=admin_token)
        if products and products.get('data'):
            print_info(f"Total products: {products['data'].get('totalElements', 0)}")
        
        if order_id:
            update_status = {"status": "PROCESSING"}
            run_test("Update order status (admin)", "PUT", f"/admin/orders/{order_id}/status", 200, data=update_status, token=admin_token)
    else:
        print_info("Skipping admin tests (no admin token)")
    
    # ==========================================
    # SUMMARY
    # ==========================================
    print_header("Test Summary")
    
    print(f"Total Tests:  {Colors.BLUE}{total_tests}{Colors.NC}")
    print(f"Passed:       {Colors.GREEN}{passed_tests}{Colors.NC}")
    print(f"Failed:       {Colors.RED}{failed_tests}{Colors.NC}")
    
    success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
    print(f"Success Rate: {Colors.CYAN}{success_rate:.1f}%{Colors.NC}")
    
    if failed_tests == 0:
        print(f"\n{Colors.GREEN}✓ All tests passed!{Colors.NC}\n")
        sys.exit(0)
    else:
        print(f"\n{Colors.RED}✗ Some tests failed{Colors.NC}\n")
        sys.exit(1)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Tests interrupted by user{Colors.NC}\n")
        sys.exit(130)
