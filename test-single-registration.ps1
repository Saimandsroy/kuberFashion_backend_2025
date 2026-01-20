# Test single registration with detailed error
$testUser = @{
    firstName = "John"
    lastName = "Doe"
    email = "johndoe123@example.com"
    phone = "9876543299"
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

Write-Host "Testing registration..." -ForegroundColor Yellow
Write-Host "Request body:" -ForegroundColor Cyan
Write-Host $testUser -ForegroundColor White

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $testUser
    Write-Host "Success!" -ForegroundColor Green
    Write-Host $response.Content
} catch {
    Write-Host "Error!" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $responseBody = $reader.ReadToEnd()
    Write-Host "Response:" -ForegroundColor Yellow
    Write-Host $responseBody -ForegroundColor White
}
