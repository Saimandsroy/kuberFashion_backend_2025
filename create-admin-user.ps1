# Create Admin User
Write-Host "=== Creating Admin User ===" -ForegroundColor Cyan

# Register admin user with strong password
$adminUser = @{
    firstName = "Admin"
    lastName = "User"
    email = "admin@kuberfashion.com"
    phone = "9999999999"
    password = "Admin@123456"
    confirmPassword = "Admin@123456"
    referralCode = ""
} | ConvertTo-Json

Write-Host "`nRegistering admin user..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $adminUser
    Write-Host "OK Admin user created successfully!" -ForegroundColor Green
    Write-Host "   Email: admin@kuberfashion.com" -ForegroundColor White
    Write-Host "   Phone: 9999999999" -ForegroundColor White
    Write-Host "   Password: Admin@123456" -ForegroundColor White
    Write-Host "`nIMPORTANT: You need to manually update the role to ADMIN in the database!" -ForegroundColor Yellow
    Write-Host "Or use the admin phone login endpoint with this user." -ForegroundColor Yellow
} catch {
    $errorMsg = $_.Exception.Message
    if ($errorMsg -like "*already exists*") {
        Write-Host "INFO Admin user already exists" -ForegroundColor Yellow
        Write-Host "`nTry logging in with:" -ForegroundColor Cyan
        Write-Host "   Email: admin@kuberfashion.com" -ForegroundColor White
        Write-Host "   Password: Admin@123456" -ForegroundColor White
    } else {
        Write-Host "FAIL Failed to create admin user: $errorMsg" -ForegroundColor Red
    }
}

Write-Host "`n=== Testing Admin Login ===" -ForegroundColor Cyan
$adminLogin = @{
    email = "admin@kuberfashion.com"
    password = "Admin@123456"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/auth/login" -Method POST -ContentType "application/json" -Body $adminLogin
    Write-Host "OK Admin login successful!" -ForegroundColor Green
    Write-Host "   Token: $($loginResponse.data.token.Substring(0, 20))..." -ForegroundColor White
} catch {
    Write-Host "FAIL Admin login failed" -ForegroundColor Red
    Write-Host "   The user may not have ADMIN role in database" -ForegroundColor Yellow
    Write-Host "   You need to manually update the role in Supabase dashboard" -ForegroundColor Yellow
}
