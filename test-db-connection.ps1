# PostgreSQL Connection Diagnostic Script
Write-Host "=== PostgreSQL Connection Diagnostics ===" -ForegroundColor Cyan

# Test 1: Check if Supabase endpoint is reachable
Write-Host "`n1. Testing Supabase endpoint connectivity..." -ForegroundColor Yellow
$supabaseHost = "db.hanmurmflpqbfwwvqnsl.supabase.co"
$supabasePort = 5432

try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $tcpClient.Connect($supabaseHost, $supabasePort)
    $tcpClient.Close()
    Write-Host "✅ Supabase host is reachable on port $supabasePort" -ForegroundColor Green
} catch {
    Write-Host "❌ Cannot reach Supabase host: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   This indicates the database is paused or terminated" -ForegroundColor Red
}

# Test 2: Check alternative Supabase pooler endpoints
Write-Host "`n2. Testing alternative Supabase pooler endpoints..." -ForegroundColor Yellow
$poolerEndpoints = @(
    "aws-0-ap-south-1.pooler.supabase.com",
    "aws-1-ap-south-1.pooler.supabase.com", 
    "db.hanmurmflpqbfwwvqnsl.supabase.co"
)

foreach ($endpoint in $poolerEndpoints) {
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.Connect($endpoint, 5432)
        $tcpClient.Close()
        Write-Host "✅ $endpoint is reachable" -ForegroundColor Green
    } catch {
        Write-Host "❌ $endpoint is not reachable" -ForegroundColor Red
    }
}

# Test 3: Check Supabase API status
Write-Host "`n3. Testing Supabase API status..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "https://hanmurmflpqbfwwvqnsl.supabase.co/rest/v1/" -Headers @{
        "apikey" = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhhbm11cm1mbHBxYmZ3d3ZxbnNsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTgwMjExMDEsImV4cCI6MjA3MzU5NzEwMX0.jPpF3Zyp33dh84eXi05VhBG8i4yu43xIzxBmGsSn6ns"
    } -TimeoutSec 10
    Write-Host "✅ Supabase API is responding" -ForegroundColor Green
} catch {
    Write-Host "❌ Supabase API is not responding: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Recommendations ===" -ForegroundColor Cyan
Write-Host "If all tests fail, your Supabase database is likely paused." -ForegroundColor Yellow
Write-Host "Solutions:" -ForegroundColor White
Write-Host "1. Login to Supabase dashboard and unpause the database" -ForegroundColor White
Write-Host "2. Switch to development mode with H2 database" -ForegroundColor White
Write-Host "3. Set up local PostgreSQL instance" -ForegroundColor White
