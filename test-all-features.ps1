# Comprehensive Backend Feature Testing
Write-Host "=== KuberFashion Backend - Complete Feature Test ===" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$testResults = @()

# Test 1: Health Check
Write-Host "`n[1/8] Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method GET
    Write-Host "OK Health Check PASSED" -ForegroundColor Green
    $testResults += "Health: PASS"
} catch {
    Write-Host "FAIL Health Check FAILED" -ForegroundColor Red
    $testResults += "Health: FAIL"
    exit 1
}

# Test 2: User Registration
Write-Host "`n[2/8] Testing User Registration..." -ForegroundColor Yellow
$randomPhone = "98765432" + (Get-Random -Minimum 10 -Maximum 99)
$testUser = @{
    firstName = "Test"
    lastName = "User"
    email = "test$randomPhone@example.com"
    phone = $randomPhone
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -ContentType "application/json" -Body $testUser
    Write-Host "OK User Registration PASSED" -ForegroundColor Green
    Write-Host "   User ID: $($registerResponse.data.user.id)" -ForegroundColor White
    Write-Host "   Kuber Coupons: $($registerResponse.data.user.kuberCoupons)" -ForegroundColor White
    $testResults += "Registration: PASS"
    $testPhone = $randomPhone
    $testPassword = "Test@123"
} catch {
    $errorMsg = $_.Exception.Message
    if ($errorMsg -like "*already exists*") {
        Write-Host "INFO User already exists (using existing for tests)" -ForegroundColor Yellow
        $testResults += "Registration: SKIP (exists)"
        $testPhone = "9876543210"
        $testPassword = "Test@123"
    } else {
        Write-Host "FAIL Registration FAILED: $errorMsg" -ForegroundColor Red
        $testResults += "Registration: FAIL"
    }
}

# Test 3: Phone Login
Write-Host "`n[3/8] Testing Phone + Password Login..." -ForegroundColor Yellow
$loginData = @{
    phone = $testPhone
    password = $testPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/phone/login" -Method POST -ContentType "application/json" -Body $loginData
    Write-Host "OK Phone Login PASSED" -ForegroundColor Green
    Write-Host "   User: $($loginResponse.data.user.firstName) $($loginResponse.data.user.lastName)" -ForegroundColor White
    Write-Host "   Token: $($loginResponse.data.token.Substring(0, 20))..." -ForegroundColor White
    $testResults += "Phone Login: PASS"
    $token = $loginResponse.data.token
} catch {
    Write-Host "FAIL Phone Login FAILED: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += "Phone Login: FAIL"
    $token = $null
}

# Test 4: Protected Endpoint (User Profile)
if ($token) {
    Write-Host "`n[4/8] Testing Protected Endpoint (User Profile)..." -ForegroundColor Yellow
    try {
        $headers = @{ "Authorization" = "Bearer $token" }
        $profileResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/profile" -Method GET -Headers $headers
        Write-Host "OK Protected Endpoint PASSED" -ForegroundColor Green
        Write-Host "   Profile: $($profileResponse.data.firstName) $($profileResponse.data.lastName)" -ForegroundColor White
        $testResults += "Protected Endpoint: PASS"
    } catch {
        Write-Host "FAIL Protected Endpoint FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $testResults += "Protected Endpoint: FAIL"
    }
} else {
    Write-Host "`n[4/8] SKIP Protected Endpoint (no token)" -ForegroundColor Yellow
    $testResults += "Protected Endpoint: SKIP"
}

# Test 5: Products Endpoint
Write-Host "`n[5/8] Testing Products Endpoint..." -ForegroundColor Yellow
try {
    $productsResponse = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method GET
    Write-Host "OK Products Endpoint PASSED" -ForegroundColor Green
    if ($productsResponse.data.content) {
        Write-Host "   Total Products: $($productsResponse.data.content.Count)" -ForegroundColor White
    } else {
        Write-Host "   No products found (empty database)" -ForegroundColor Yellow
    }
    $testResults += "Products: PASS"
} catch {
    Write-Host "FAIL Products Endpoint FAILED: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += "Products: FAIL"
}

# Test 6: Categories Endpoint
Write-Host "`n[6/8] Testing Categories Endpoint..." -ForegroundColor Yellow
try {
    $categoriesResponse = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method GET
    Write-Host "OK Categories Endpoint PASSED" -ForegroundColor Green
    if ($categoriesResponse.data) {
        Write-Host "   Total Categories: $($categoriesResponse.data.Count)" -ForegroundColor White
    }
    $testResults += "Categories: PASS"
} catch {
    Write-Host "FAIL Categories Endpoint FAILED: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += "Categories: FAIL"
}

# Test 7: Admin Login
Write-Host "`n[7/8] Testing Admin Login..." -ForegroundColor Yellow
$adminLogin = @{
    email = "admin@kuberfashion.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminLoginResponse = Invoke-RestMethod -Uri "$baseUrl/api/admin/auth/login" -Method POST -ContentType "application/json" -Body $adminLogin
    Write-Host "OK Admin Login PASSED" -ForegroundColor Green
    Write-Host "   Admin: $($adminLoginResponse.data.user.firstName) $($adminLoginResponse.data.user.lastName)" -ForegroundColor White
    $testResults += "Admin Login: PASS"
    $adminToken = $adminLoginResponse.data.token
} catch {
    Write-Host "FAIL Admin Login FAILED: $($_.Exception.Message)" -ForegroundColor Red
    $testResults += "Admin Login: FAIL"
    $adminToken = $null
}

# Test 8: Referral System
if ($adminToken) {
    Write-Host "`n[8/8] Testing Referral System (Admin Endpoint)..." -ForegroundColor Yellow
    try {
        $headers = @{ "Authorization" = "Bearer $adminToken" }
        $referralResponse = Invoke-RestMethod -Uri "$baseUrl/api/admin/referrals/tree" -Method GET -Headers $headers
        Write-Host "OK Referral System PASSED" -ForegroundColor Green
        if ($referralResponse.data) {
            Write-Host "   Total Referral Nodes: $($referralResponse.data.Count)" -ForegroundColor White
        }
        $testResults += "Referral System: PASS"
    } catch {
        Write-Host "FAIL Referral System FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $testResults += "Referral System: FAIL"
    }
} else {
    Write-Host "`n[8/8] SKIP Referral System (no admin token)" -ForegroundColor Yellow
    $testResults += "Referral System: SKIP"
}

# Summary
Write-Host "`n=== Test Summary ===" -ForegroundColor Cyan
$testResults | ForEach-Object { Write-Host "  $_" -ForegroundColor White }

$passCount = ($testResults | Where-Object { $_ -like "*PASS*" }).Count
$failCount = ($testResults | Where-Object { $_ -like "*FAIL*" }).Count
$skipCount = ($testResults | Where-Object { $_ -like "*SKIP*" }).Count

Write-Host "`nResults: $passCount passed, $failCount failed, $skipCount skipped" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })

if ($failCount -eq 0) {
    Write-Host "`nOK All tests passed! Backend is working correctly." -ForegroundColor Green
} else {
    Write-Host "`nWARNING Some tests failed. Check errors above." -ForegroundColor Yellow
}
