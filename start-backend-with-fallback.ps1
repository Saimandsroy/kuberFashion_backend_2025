# Start KuberFashion Backend with Fallback
Write-Host "=== Starting KuberFashion Backend ===" -ForegroundColor Cyan

# Stop existing Java processes
Write-Host "`nStopping existing Java processes..." -ForegroundColor Yellow
taskkill /F /IM java.exe 2>$null | Out-Null
Start-Sleep -Seconds 2

# Test Supabase connectivity
Write-Host "`nTesting Supabase database..." -ForegroundColor Yellow
$supabaseTest = Test-NetConnection -ComputerName "aws-0-ap-south-1.pooler.supabase.com" -Port 5432 -WarningAction SilentlyContinue

if ($supabaseTest.TcpTestSucceeded) {
    Write-Host "OK Supabase is reachable - Starting in PRODUCTION mode" -ForegroundColor Green
    
    # Set production environment
    $env:SPRING_PROFILES_ACTIVE = "prod"
    $env:DATABASE_URL = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&connectTimeout=60&socketTimeout=60&loginTimeout=60&tcpKeepAlive=true"
    $env:DATABASE_USERNAME = "postgres.hanmurmflpqbfwwvqnsl"
    $env:DATABASE_PASSWORD = "saimandsroy2005@"
    $env:JWT_SECRET = "u5PS40PD1gBpFFAgD7ugXd6k9klL+h9YZMv3gIjKH3Nof/qaJf2rPbr0wrwwe6WaDfq7q3JOjwdBF/6AaKz7sQ=="
    $env:SERVER_PORT = "8080"
    
    Write-Host "Starting with PostgreSQL (Supabase)..." -ForegroundColor Cyan
    Write-Host "This may take 30-60 seconds for SSL handshake..." -ForegroundColor White
    Write-Host ""
    
} else {
    Write-Host "WARNING Supabase not reachable - Starting in DEVELOPMENT mode with H2" -ForegroundColor Yellow
    
    # Set development environment
    $env:SPRING_PROFILES_ACTIVE = "dev"
    $env:SERVER_PORT = "8080"
    
    Write-Host "Starting with H2 in-memory database..." -ForegroundColor Cyan
    Write-Host "H2 Console: http://localhost:8080/h2-console" -ForegroundColor White
    Write-Host "JDBC URL: jdbc:h2:mem:kuberfashion_dev" -ForegroundColor White
    Write-Host ""
}

# Start backend
mvn spring-boot:run

Write-Host "`nBackend stopped." -ForegroundColor Yellow
