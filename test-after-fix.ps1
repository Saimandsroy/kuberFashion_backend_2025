# Test Backend After Database Fix
Write-Host "Testing Backend After kuber_coupons Fix" -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
    Write-Host "OK Health Check: PASSED" -ForegroundColor Green
} catch {
    Write-Host "FAIL Health Check: Backend not running" -ForegroundColor Red
    exit 1
}

# Test 2: User Registration (This should work after fix)
Write-Host "`n2. Testing User Registration..." -ForegroundColor Yellow
$testUser = @{
    firstName = "Test"
    lastName = "Fix"
    email = "testfix@example.com"
    phone = "9876543299"
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $testUser
    Write-Host "OK User Registration: PASSED" -ForegroundColor Green
    Write-Host "   User ID: $($registerResponse.data.user.id)" -ForegroundColor White
    Write-Host "   Kuber Coupons: $($registerResponse.data.user.kuberCoupons)" -ForegroundColor White
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "INFO User already exists (expected if run multiple times)" -ForegroundColor Yellow
    } else {
        Write-Host "FAIL User Registration: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "   This indicates the kuber_coupons column is still missing" -ForegroundColor Red
    }
}

# Test 3: Phone Login
Write-Host "`n3. Testing Phone Login..." -ForegroundColor Yellow
$loginData = @{
    phone = "9876543299"
    password = "Test@123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/phone/login" -Method POST -ContentType "application/json" -Body $loginData
    Write-Host "OK Phone Login: PASSED" -ForegroundColor Green
    Write-Host "   User: $($loginResponse.data.user.firstName) $($loginResponse.data.user.lastName)" -ForegroundColor White
    Write-Host "   Kuber Coupons: $($loginResponse.data.user.kuberCoupons)" -ForegroundColor White
} catch {
    Write-Host "FAIL Phone Login: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nIf registration/login still fails with 'column does not exist':" -ForegroundColor Yellow
Write-Host "1. Execute the SQL fix in Supabase dashboard" -ForegroundColor White
Write-Host "2. Restart the backend" -ForegroundColor White
Write-Host "3. Run this test again" -ForegroundColor White
