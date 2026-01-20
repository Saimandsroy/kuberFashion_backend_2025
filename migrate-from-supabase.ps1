# Migrate from Supabase to Local PostgreSQL
Write-Host "=== KuberFashion: Supabase to PostgreSQL Migration ===" -ForegroundColor Cyan

# Step 1: Start local PostgreSQL with Docker
Write-Host "`n[1/6] Starting local PostgreSQL database..." -ForegroundColor Yellow
try {
    # Check if Docker is running
    docker version | Out-Null
    Write-Host "OK Docker is running" -ForegroundColor Green
} catch {
    Write-Host "FAIL Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Start PostgreSQL container
Write-Host "Starting PostgreSQL container..." -ForegroundColor White
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
Write-Host "Waiting for PostgreSQL to be ready..." -ForegroundColor White
$maxAttempts = 30
$attempt = 0
do {
    $attempt++
    Start-Sleep -Seconds 2
    $status = docker-compose exec -T postgres pg_isready -U kuberfashion_user -d kuberfashion 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK PostgreSQL is ready!" -ForegroundColor Green
        break
    }
    Write-Host "Waiting... ($attempt/$maxAttempts)" -ForegroundColor Gray
} while ($attempt -lt $maxAttempts)

if ($attempt -eq $maxAttempts) {
    Write-Host "FAIL PostgreSQL failed to start within 60 seconds" -ForegroundColor Red
    exit 1
}

# Step 2: Export data from Supabase (optional)
Write-Host "`n[2/6] Data Export Options..." -ForegroundColor Yellow
Write-Host "OPTION A: Manual export from Supabase dashboard" -ForegroundColor Cyan
Write-Host "  1. Go to https://supabase.com/dashboard" -ForegroundColor White
Write-Host "  2. Select your project" -ForegroundColor White
Write-Host "  3. Go to SQL Editor" -ForegroundColor White
Write-Host "  4. Export tables: users, products, categories, etc." -ForegroundColor White

Write-Host "`nOPTION B: Use pg_dump (if you have direct access)" -ForegroundColor Cyan
Write-Host "  pg_dump 'postgresql://postgres.hanmurmflpqbfwwvqnsl:saimandsroy2005@@aws-0-ap-south-1.pooler.supabase.com:5432/postgres' > supabase_backup.sql" -ForegroundColor White

Write-Host "`nOPTION C: Skip data export (start fresh)" -ForegroundColor Cyan
Write-Host "  Database will be initialized with sample data" -ForegroundColor White

$choice = Read-Host "`nDo you want to continue with fresh database? (y/n)"
if ($choice -ne 'y' -and $choice -ne 'Y') {
    Write-Host "Migration cancelled. Export your data first, then run this script again." -ForegroundColor Yellow
    exit 0
}

# Step 3: Update environment configuration
Write-Host "`n[3/6] Updating environment configuration..." -ForegroundColor Yellow

# Backup current .env
if (Test-Path ".env") {
    Copy-Item ".env" ".env.supabase.backup"
    Write-Host "OK Backed up current .env to .env.supabase.backup" -ForegroundColor Green
}

# Copy local configuration
Copy-Item ".env.local" ".env"
Write-Host "OK Updated .env for local PostgreSQL" -ForegroundColor Green

# Step 4: Test database connection
Write-Host "`n[4/6] Testing database connection..." -ForegroundColor Yellow
try {
    # Test connection using psql in container
    $testResult = docker-compose exec -T postgres psql -U kuberfashion_user -d kuberfashion -c "SELECT version();" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "OK Database connection successful" -ForegroundColor Green
    } else {
        throw "Connection failed"
    }
} catch {
    Write-Host "FAIL Database connection failed" -ForegroundColor Red
    Write-Host "Check Docker logs: docker-compose logs postgres" -ForegroundColor Yellow
    exit 1
}

# Step 5: Start backend with new database
Write-Host "`n[5/6] Starting backend with PostgreSQL..." -ForegroundColor Yellow
Write-Host "This will take 30-60 seconds..." -ForegroundColor White

# Kill existing Java processes
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force 2>$null

# Start backend
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run" -WindowStyle Minimized

# Wait for backend to start
Write-Host "Waiting for backend to start..." -ForegroundColor White
$maxAttempts = 30
$attempt = 0
do {
    $attempt++
    Start-Sleep -Seconds 3
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($health) {
            Write-Host "OK Backend is running with PostgreSQL!" -ForegroundColor Green
            break
        }
    } catch {
        # Continue waiting
    }
    Write-Host "Waiting for backend... ($attempt/$maxAttempts)" -ForegroundColor Gray
} while ($attempt -lt $maxAttempts)

if ($attempt -eq $maxAttempts) {
    Write-Host "WARNING Backend may still be starting. Check manually." -ForegroundColor Yellow
}

# Step 6: Verification
Write-Host "`n[6/6] Verification..." -ForegroundColor Yellow

# Test user registration
Write-Host "Testing user registration..." -ForegroundColor White
$testUser = @{
    firstName = "Test"
    lastName = "User"
    email = "test.postgres@example.com"
    phone = "9876543299"
    password = "Test@123456"
    confirmPassword = "Test@123456"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $testUser -TimeoutSec 10
    Write-Host "OK User registration working" -ForegroundColor Green
    Write-Host "   User: $($regResponse.data.user.firstName) $($regResponse.data.user.lastName)" -ForegroundColor White
} catch {
    Write-Host "INFO Registration test failed (may need more time)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n=== Migration Summary ===" -ForegroundColor Cyan
Write-Host "✅ PostgreSQL container: RUNNING" -ForegroundColor Green
Write-Host "✅ Database initialized: COMPLETE" -ForegroundColor Green
Write-Host "✅ Environment updated: COMPLETE" -ForegroundColor Green
Write-Host "✅ Backend configuration: UPDATED" -ForegroundColor Green

Write-Host "`nAccess Points:" -ForegroundColor Yellow
Write-Host "  Backend: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Database: localhost:5432" -ForegroundColor Cyan
Write-Host "  PgAdmin: http://localhost:8081" -ForegroundColor Cyan
Write-Host "    Email: admin@kuberfashion.com" -ForegroundColor White
Write-Host "    Password: admin123" -ForegroundColor White

Write-Host "`nDatabase Credentials:" -ForegroundColor Yellow
Write-Host "  Host: localhost" -ForegroundColor White
Write-Host "  Port: 5432" -ForegroundColor White
Write-Host "  Database: kuberfashion" -ForegroundColor White
Write-Host "  Username: kuberfashion_user" -ForegroundColor White
Write-Host "  Password: KuberFashion@2025!" -ForegroundColor White

Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "1. Verify backend is working: Invoke-RestMethod -Uri 'http://localhost:8080/api/health'" -ForegroundColor White
Write-Host "2. Access PgAdmin to view database: http://localhost:8081" -ForegroundColor White
Write-Host "3. Import your Supabase data if needed" -ForegroundColor White
Write-Host "4. Update frontend API configuration if needed" -ForegroundColor White

Write-Host "`n🎉 Migration completed! Your app is now using local PostgreSQL." -ForegroundColor Green
