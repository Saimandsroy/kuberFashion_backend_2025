# Test Backend Script
Write-Host "Testing KuberFashion Backend..." -ForegroundColor Green

# Test health endpoint
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
    Write-Host "✅ Backend Health Check: PASSED" -ForegroundColor Green
    Write-Host "Service: $($health.service), Version: $($health.version)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Backend Health Check: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test registration with global referral code
Write-Host "`nTesting user registration with global referral code..." -ForegroundColor Yellow
$registerData = @{
    firstName = "Test"
    lastName = "User"
    email = "test@example.com"
    phone = "9876543210"
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $registerData
    Write-Host "✅ User Registration: PASSED" -ForegroundColor Green
    Write-Host "User ID: $($registerResponse.data.user.id)" -ForegroundColor Cyan
} catch {
    Write-Host "⚠️  User Registration: User might already exist" -ForegroundColor Yellow
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Test phone login
Write-Host "`nTesting phone login..." -ForegroundColor Yellow
$loginData = @{
    phone = "9876543210"
    password = "Test@123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/phone/login" -Method POST -ContentType "application/json" -Body $loginData
    Write-Host "✅ Phone Login: PASSED" -ForegroundColor Green
    Write-Host "Token received: $($loginResponse.data.token.Substring(0, 20))..." -ForegroundColor Cyan
} catch {
    Write-Host "❌ Phone Login: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎉 Backend testing completed!" -ForegroundColor Green
Write-Host "Backend is running on: http://localhost:8080" -ForegroundColor Cyan
Write-Host "Swagger UI available at: http://localhost:8080/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "H2 Console available at: http://localhost:8080/h2-console" -ForegroundColor Cyan
