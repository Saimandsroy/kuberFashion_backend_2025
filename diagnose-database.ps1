# Comprehensive Database Connection Diagnostics
param(
    [string]$Mode = "supabase",  # supabase, local, docker
    [switch]$Verbose
)

Write-Host "=== KuberFashion Database Diagnostics ===" -ForegroundColor Cyan
Write-Host "Mode: $Mode" -ForegroundColor Yellow

function Test-Port {
    param($Host, $Port, $Timeout = 5000)
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $asyncResult = $tcpClient.BeginConnect($Host, $Port, $null, $null)
        $wait = $asyncResult.AsyncWaitHandle.WaitOne($Timeout, $false)
        if ($wait) {
            $tcpClient.EndConnect($asyncResult)
            $tcpClient.Close()
            return $true
        } else {
            $tcpClient.Close()
            return $false
        }
    } catch {
        return $false
    }
}

function Test-DatabaseConnection {
    param($ConnectionString, $Description)
    Write-Host "`nTesting: $Description" -ForegroundColor Yellow
    
    # This would require PostgreSQL .NET driver for full testing
    # For now, we'll test the host/port connectivity
    if ($ConnectionString -match "://([^:]+):(\d+)/") {
        $host = $matches[1]
        $port = [int]$matches[2]
        
        if (Test-Port -Host $host -Port $port) {
            Write-Host "✅ $host:$port is reachable" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $host:$port is not reachable" -ForegroundColor Red
            return $false
        }
    }
    return $false
}

# Test based on mode
switch ($Mode) {
    "supabase" {
        Write-Host "`n1. Testing Supabase Connections..." -ForegroundColor Cyan
        
        $supabaseConnections = @(
            @{
                Url = "jdbc:postgresql://db.hanmurmflpqbfwwvqnsl.supabase.co:5432/postgres"
                Description = "Direct Supabase Connection"
            },
            @{
                Url = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres"
                Description = "Supabase Pooler (aws-0)"
            },
            @{
                Url = "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres"
                Description = "Supabase Pooler (aws-1)"
            }
        )
        
        $workingConnection = $null
        foreach ($conn in $supabaseConnections) {
            if (Test-DatabaseConnection -ConnectionString $conn.Url -Description $conn.Description) {
                $workingConnection = $conn.Url
                break
            }
        }
        
        if ($workingConnection) {
            Write-Host "`n✅ Found working Supabase connection: $workingConnection" -ForegroundColor Green
            Write-Host "Update your .env file with this URL" -ForegroundColor Yellow
        } else {
            Write-Host "`n❌ No Supabase connections are working" -ForegroundColor Red
            Write-Host "Recommendations:" -ForegroundColor Yellow
            Write-Host "1. Check Supabase dashboard - database might be paused" -ForegroundColor White
            Write-Host "2. Verify your Supabase project is active" -ForegroundColor White
            Write-Host "3. Switch to development mode: set SPRING_PROFILES_ACTIVE=dev" -ForegroundColor White
        }
    }
    
    "local" {
        Write-Host "`n1. Testing Local PostgreSQL..." -ForegroundColor Cyan
        Test-DatabaseConnection -ConnectionString "jdbc:postgresql://localhost:5432/kuberfashion" -Description "Local PostgreSQL"
    }
    
    "docker" {
        Write-Host "`n1. Testing Docker PostgreSQL..." -ForegroundColor Cyan
        Test-DatabaseConnection -ConnectionString "jdbc:postgresql://localhost:5432/kuberfashion" -Description "Docker PostgreSQL"
        
        Write-Host "`n2. Checking Docker containers..." -ForegroundColor Cyan
        try {
            $containers = docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Where-Object { $_ -match "postgres|kuberfashion" }
            if ($containers) {
                Write-Host "Docker containers:" -ForegroundColor Green
                $containers | ForEach-Object { Write-Host $_ -ForegroundColor White }
            } else {
                Write-Host "❌ No relevant Docker containers found" -ForegroundColor Red
            }
        } catch {
            Write-Host "❌ Docker not available or not running" -ForegroundColor Red
        }
    }
}

Write-Host "`n=== System Information ===" -ForegroundColor Cyan
Write-Host "OS: $($env:OS)" -ForegroundColor White
Write-Host "Java Version:" -ForegroundColor White
try {
    java -version 2>&1 | Select-Object -First 1 | Write-Host -ForegroundColor White
} catch {
    Write-Host "❌ Java not found in PATH" -ForegroundColor Red
}

Write-Host "`nMaven Version:" -ForegroundColor White
try {
    mvn -version | Select-Object -First 1 | Write-Host -ForegroundColor White
} catch {
    Write-Host "❌ Maven not found in PATH" -ForegroundColor Red
}

Write-Host "`n=== Recommended Actions ===" -ForegroundColor Cyan
Write-Host "1. For immediate development: Use H2 database" -ForegroundColor Yellow
Write-Host "   Command: set SPRING_PROFILES_ACTIVE=dev && mvn spring-boot:run" -ForegroundColor White

Write-Host "`n2. For production with Supabase:" -ForegroundColor Yellow
Write-Host "   - Login to Supabase dashboard" -ForegroundColor White
Write-Host "   - Check if database is paused and unpause it" -ForegroundColor White
Write-Host "   - Verify connection settings" -ForegroundColor White

Write-Host "`n3. For local PostgreSQL setup:" -ForegroundColor Yellow
Write-Host "   - Install PostgreSQL locally" -ForegroundColor White
Write-Host "   - Or use Docker: docker-compose up postgres" -ForegroundColor White

Write-Host "`n4. For Docker deployment:" -ForegroundColor Yellow
Write-Host "   - docker-compose up --build" -ForegroundColor White
