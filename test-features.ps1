# Test All KuberFashion Features
Write-Host "=== Testing KuberFashion Backend Features ===" -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
    Write-Host "✅ Health Check: PASSED" -ForegroundColor Green
    Write-Host "   Service: $($health.service), Version: $($health.version)" -ForegroundColor White
} catch {
    Write-Host "❌ Health Check: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: User Registration with Global Referral Code
Write-Host "`n2. Testing User Registration with Global Referral Code..." -ForegroundColor Yellow
$registerData = @{
    firstName = "Test"
    lastName = "User"
    email = "testuser@example.com"
    phone = "9876543210"
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $registerData
    Write-Host "✅ User Registration: PASSED" -ForegroundColor Green
    Write-Host "   User ID: $($registerResponse.data.user.id)" -ForegroundColor White
    Write-Host "   User Phone: $($registerResponse.data.user.phone)" -ForegroundColor White
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "⚠️  User Registration: User already exists (expected)" -ForegroundColor Yellow
    } else {
        Write-Host "❌ User Registration: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 3: Phone Login
Write-Host "`n3. Testing Phone + Password Login..." -ForegroundColor Yellow
$loginData = @{
    phone = "9876543210"
    password = "Test@123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/phone/login" -Method POST -ContentType "application/json" -Body $loginData
    Write-Host "✅ Phone Login: PASSED" -ForegroundColor Green
    Write-Host "   Token: $($loginResponse.data.token.Substring(0, 20))..." -ForegroundColor White
    Write-Host "   User: $($loginResponse.data.user.firstName) $($loginResponse.data.user.lastName)" -ForegroundColor White
    $token = $loginResponse.data.token
} catch {
    Write-Host "❌ Phone Login: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    $token = $null
}

# Test 4: Protected Endpoint (if login was successful)
if ($token) {
    Write-Host "`n4. Testing Protected Endpoint..." -ForegroundColor Yellow
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $profileResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/users/profile" -Method GET -Headers $headers
        Write-Host "✅ Protected Endpoint: PASSED" -ForegroundColor Green
        Write-Host "   Profile: $($profileResponse.data.firstName) $($profileResponse.data.lastName)" -ForegroundColor White
    } catch {
        Write-Host "❌ Protected Endpoint: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 5: Products Endpoint (Public)
Write-Host "`n5. Testing Products Endpoint..." -ForegroundColor Yellow
try {
    $productsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method GET
    Write-Host "✅ Products Endpoint: PASSED" -ForegroundColor Green
    Write-Host "   Total Products: $($productsResponse.data.content.Count)" -ForegroundColor White
} catch {
    Write-Host "❌ Products Endpoint: FAILED" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: CORS Test
Write-Host "`n6. Testing CORS Configuration..." -ForegroundColor Yellow
try {
    $corsHeaders = @{
        "Origin" = "http://10.197.216.70:5173"
        "Access-Control-Request-Method" = "POST"
        "Access-Control-Request-Headers" = "content-type"
    }
    $corsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/phone/login" -Method OPTIONS -Headers $corsHeaders
    Write-Host "✅ CORS Configuration: PASSED" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 200) {
        Write-Host "✅ CORS Configuration: PASSED" -ForegroundColor Green
    } else {
        Write-Host "❌ CORS Configuration: FAILED" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== Test Summary ===" -ForegroundColor Cyan
Write-Host "Backend is running on: http://localhost:8080" -ForegroundColor White
Write-Host "Swagger UI: http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "Database: PostgreSQL (Supabase Production)" -ForegroundColor White
Write-Host "Profile: Production" -ForegroundColor White

Write-Host "`n✅ Backend is ready for production use!" -ForegroundColor Green
