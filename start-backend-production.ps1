# Start KuberFashion Backend in Production Mode
Write-Host "=== Starting KuberFashion Backend (Production) ===" -ForegroundColor Cyan

# Check if port 8080 is already in use
Write-Host "`nChecking if port 8080 is available..." -ForegroundColor Yellow
$portCheck = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "Port 8080 is already in use. Stopping existing Java processes..." -ForegroundColor Yellow
    taskkill /F /IM java.exe 2>$null
    Start-Sleep -Seconds 3
}

# Set environment variables
Write-Host "`nSetting environment variables..." -ForegroundColor Yellow
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/kuberfashion?sslmode=disable"
$env:DATABASE_USERNAME = "kuberfashion_user"
$env:DATABASE_PASSWORD = "KuberFashion@2025!"
$env:JWT_SECRET = "u5PS40PD1gBpFFAgD7ugXd6k9klL+h9YZMv3gIjKH3Nof/qaJf2rPbr0wrwwe6WaDfq7q3JOjwdBF/6AaKz7sQ=="
$env:SERVER_PORT = "8080"

Write-Host "OK Environment configured" -ForegroundColor Green

# Test database connectivity
Write-Host "`nTesting database connectivity..." -ForegroundColor Yellow
$dbTest = Test-NetConnection -ComputerName "localhost" -Port 5432 -WarningAction SilentlyContinue
if ($dbTest.TcpTestSucceeded) {
    Write-Host "OK Database is reachable" -ForegroundColor Green
} else {
    Write-Host "WARNING Database connection test failed" -ForegroundColor Red
    Write-Host "Backend may fail to start. Ensure PostgreSQL is running (docker-compose up -d postgres)." -ForegroundColor Red
}

# Start backend
Write-Host "`nStarting backend..." -ForegroundColor Yellow
Write-Host "This will take 20-30 seconds..." -ForegroundColor White
Write-Host ""

mvn spring-boot:run

# Note: The above command blocks, so the lines below won't execute until you stop it
Write-Host "`nBackend stopped." -ForegroundColor Yellow
