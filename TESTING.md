# KuberFashion Backend Testing Guide

This guide explains how to run automated tests for the KuberFashion backend API.

## Test Scripts

We provide two automated test scripts:

1. **`test-backend.sh`** - Bash script (works on macOS/Linux, requires `curl`)
2. **`test-backend.py`** - Python script (requires Python 3 and `requests` library)

Both scripts test the same endpoints and provide colored output for easy reading.

## Prerequisites

### For Bash Script (`test-backend.sh`)
- `curl` (pre-installed on macOS/Linux)
- `bash` shell

### For Python Script (`test-backend.py`)
- Python 3.6+
- `requests` library

Install Python dependencies:
```bash
pip3 install requests
```

## Running Tests

### 1. Start the Backend

First, ensure your backend is running. Choose one:

**Option A: Local with H2 (dev mode)**
```bash
./run-dev.sh
# or
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Option B: Local with PostgreSQL (prod mode)**
```bash
# Start PostgreSQL
docker compose up -d

# Load environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/kuberfashion?sslmode=disable
export DATABASE_USERNAME=kuberfashion_user
export DATABASE_PASSWORD=KuberFashion@2025!
export JWT_SECRET=u5PS40PD1gBpFFAgD7ugXd6k9klL+h9YZMv3gIjKH3Nof/qaJf2rPbr0wrwwe6WaDfq7q3JOjwdBF/6AaKz7sQ==

# Run backend
mvn spring-boot:run
```

**Option C: Production server**
```bash
# Backend should already be running at https://api.kuberfashions.in
```

### 2. Run the Test Script

**Bash version:**
```bash
# Test local backend
./test-backend.sh

# Test local backend (explicit URL)
./test-backend.sh http://localhost:8080/api

# Test production backend
./test-backend.sh https://api.kuberfashions.in/api
```

**Python version:**
```bash
# Test local backend
python3 test-backend.py

# Test local backend (explicit URL)
python3 test-backend.py http://localhost:8080/api

# Test production backend
python3 test-backend.py https://api.kuberfashions.in/api
```

## What Gets Tested

The automated tests cover:

### 1. Health Check
- ✓ GET `/api/health` - Verify service is up

### 2. Public Endpoints
- ✓ GET `/api/categories` - List all categories
- ✓ GET `/api/products` - List all products
- ✓ GET `/api/products/featured` - Featured products
- ✓ GET `/api/products/newest` - Newest products
- ✓ GET `/api/products/search?q=shirt` - Search products

### 3. User Registration & Authentication
- ✓ POST `/api/auth/register` - Register new user
- ✓ POST `/api/auth/login` - Login user
- ✓ GET `/api/auth/me` - Get current user info

### 4. Cart Operations (Authenticated)
- ✓ GET `/api/cart` - Get cart
- ✓ POST `/api/cart/add` - Add item to cart
- ✓ PUT `/api/cart/items/{id}` - Update cart item quantity
- ✓ GET `/api/cart/count` - Get cart item count
- ✓ DELETE `/api/cart/items/{id}` - Remove item from cart
- ✓ DELETE `/api/cart` - Clear cart

### 5. Wishlist Operations (Authenticated)
- ✓ GET `/api/wishlist` - Get wishlist
- ✓ POST `/api/wishlist/add` - Add item to wishlist
- ✓ DELETE `/api/wishlist/remove/{id}` - Remove item from wishlist

### 6. Order Operations (Authenticated)
- ✓ POST `/api/orders/create` - Create order
- ✓ GET `/api/orders/{id}` - Get order by ID
- ✓ GET `/api/orders/my-orders` - Get user orders

### 7. Admin Authentication
- ✓ POST `/api/admin/auth/login` - Admin login

### 8. Admin Operations (Admin role required)
- ✓ GET `/api/admin/users` - List all users
- ✓ GET `/api/admin/orders` - List all orders
- ✓ GET `/api/admin/products` - List all products
- ✓ PUT `/api/admin/orders/{id}/status` - Update order status

## Test Output

The scripts provide colored output:
- 🟢 **Green** - Test passed
- 🔴 **Red** - Test failed
- 🟡 **Yellow** - Test running / Info
- 🔵 **Blue** - Section headers / Info

Example output:
```
========================================
        1. Health Check
========================================

TEST: Health endpoint
✓ PASS: Health endpoint (Status: 200)

========================================
        Test Summary
========================================

Total Tests:  35
Passed:       35
Failed:       0

✓ All tests passed!
```

## Configuration

### Admin Credentials

By default, tests use:
- **Email:** `admin@kuberfashion.com`
- **Password:** `admin123`

To use different admin credentials, set environment variables:
```bash
export ADMIN_EMAIL=your-admin@example.com
export ADMIN_PASSWORD=your-password
```

### Test User

The scripts automatically create a unique test user for each run using:
- **Email:** `test_<timestamp>@example.com`
- **Password:** `TestPass123!`
- **Phone:** `+91<timestamp>`

## Troubleshooting

### Connection Refused
```
✗ FAIL: Health endpoint
  Details: Connection refused. Is the server running at http://localhost:8080/api?
```

**Solution:** Ensure the backend is running on the correct port.

### Admin Login Failed
```
ℹ INFO: Admin login failed. Create admin with: POST /api/auth/create-admin?email=admin@kuberfashion.com
```

**Solution:** Create an admin user first:
```bash
curl -X POST "http://localhost:8080/api/auth/create-admin?email=admin@kuberfashion.com"
```

### Database Connection Error
```
✗ FAIL: Register new user
  Details: Expected status 201, got 500
```

**Solution:** 
- Check if PostgreSQL is running: `docker compose ps`
- Verify database credentials in `.env` file
- Check backend logs for detailed error

### Port Already in Use
```
Error: Port 5432 is already in use
```

**Solution:**
```bash
# Check what's using the port
lsof -i :5432

# Stop existing PostgreSQL
brew services stop postgresql
# or
docker compose down
```

## CI/CD Integration

You can integrate these tests into your CI/CD pipeline:

**GitHub Actions example:**
```yaml
- name: Run Backend Tests
  run: |
    docker compose up -d
    sleep 10  # Wait for DB to be ready
    export SPRING_PROFILES_ACTIVE=prod
    export DATABASE_URL=jdbc:postgresql://localhost:5432/kuberfashion?sslmode=disable
    export DATABASE_USERNAME=kuberfashion_user
    export DATABASE_PASSWORD=KuberFashion@2025!
    mvn spring-boot:run &
    sleep 30  # Wait for backend to start
    python3 test-backend.py
```

## Manual Testing with Postman

For manual testing and more detailed exploration, see:
- **[test.md](./test.md)** - Comprehensive Postman test guide

## Quick Test Commands

```bash
# Full test suite (local)
./test-backend.sh

# Full test suite (production)
./test-backend.sh https://api.kuberfashions.in/api

# Quick health check
curl http://localhost:8080/api/health

# Quick product list
curl http://localhost:8080/api/products

# Test with verbose output (Python)
python3 test-backend.py | tee test-results.log
```

## Exit Codes

- **0** - All tests passed
- **1** - Some tests failed
- **130** - Tests interrupted by user (Ctrl+C)

## Support

If tests fail consistently:
1. Check backend logs: `docker logs kuberfashion-postgres`
2. Verify environment variables are set correctly
3. Ensure all required services (DB, backend) are running
4. Check firewall/network settings
5. Review `TESTING.md` and `test.md` for detailed setup

## Next Steps

After successful testing:
1. Deploy to production server
2. Run tests against production URL
3. Set up monitoring and alerts
4. Configure frontend to use production API
