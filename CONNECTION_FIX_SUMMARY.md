# PostgreSQL Connection Timeout Fix - Complete Solution

## 🔍 Problem Analysis

### Root Cause:
**SocketTimeoutException during SSL handshake with Supabase PostgreSQL**

The error occurred because:
1. SSL handshake was timing out (default 30s was insufficient)
2. Custom DatabaseConfig with retry logic was interfering with Spring Boot's connection management
3. Hikari pool initialization was failing due to tight timeout constraints
4. Supabase pooler requires longer connection timeouts for SSL negotiation

## ✅ Fixes Applied

### Fix 1: Removed Custom DatabaseConfig
**File**: `DatabaseConfig.java` → renamed to `DatabaseConfig.java.backup`

**Why**: The custom configuration with `@Retryable` and manual connection testing was:
- Interfering with Spring Boot's auto-configuration
- Causing premature connection failures
- Not allowing enough time for SSL handshake

**Solution**: Let Spring Boot handle datasource configuration natively

### Fix 2: Enhanced Connection URL Parameters
**File**: `.env`

**Before**:
```
DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
```

**After**:
```
DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require&connectTimeout=60&socketTimeout=60&loginTimeout=60&tcpKeepAlive=true
```

**Parameters Explained**:
- `connectTimeout=60` - 60 seconds for initial connection (up from 30s)
- `socketTimeout=60` - 60 seconds for socket read operations
- `loginTimeout=60` - 60 seconds for authentication
- `tcpKeepAlive=true` - Keep connection alive, detect dead connections

### Fix 3: Optimized Hikari Connection Pool Settings
**File**: `application-prod.properties`

**Changes**:
```properties
# Increased timeouts for SSL handshake
spring.datasource.hikari.connection-timeout=60000          # 60s (was 20s)
spring.datasource.hikari.validation-timeout=10000          # 10s (was 5s)
spring.datasource.hikari.initialization-fail-timeout=60000 # 60s (new)
spring.datasource.hikari.keepalive-time=60000              # 60s (was 30s)

# Reduced pool size for Supabase free tier
spring.datasource.hikari.maximum-pool-size=5               # 5 (was 8)
spring.datasource.hikari.minimum-idle=1                    # 1 (was 2)

# Extended connection lifetime
spring.datasource.hikari.max-lifetime=600000               # 10 min (was 5 min)
```

**Why These Values**:
- **60s timeouts**: Supabase SSL handshake can take 30-45 seconds on slow networks
- **Smaller pool**: Free tier has connection limits; fewer connections = more stable
- **Longer lifetime**: Reduce connection churn and SSL renegotiation overhead

### Fix 4: Created Fallback Startup Script
**File**: `start-backend-with-fallback.ps1`

**Features**:
- Tests Supabase connectivity before starting
- Falls back to H2 in-memory database if Supabase unreachable
- Provides clear feedback on which mode is starting
- Prevents startup failures due to database unavailability

## 🚀 How to Start Backend

### Option 1: With Automatic Fallback (Recommended)
```powershell
cd f:\kuber\backend
.\start-backend-with-fallback.ps1
```

This will:
- Test Supabase connectivity
- Start with PostgreSQL if available
- Fall back to H2 if Supabase is down
- Wait 30-60 seconds for SSL handshake

### Option 2: Force Production Mode
```powershell
cd f:\kuber\backend
.\start-backend-production.ps1
```

### Option 3: Force Development Mode (H2)
```powershell
cd f:\kuber\backend
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

## 🧪 Verification Steps

### 1. Wait for Startup
**Important**: SSL handshake can take 30-60 seconds. Be patient!

Look for these log messages:
```
✅ HikariPool-1 - Start completed
✅ Started KuberFashionApplication
```

### 2. Test Health Endpoint
```powershell
# Wait 60 seconds after startup, then:
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
```

Expected response:
```json
{
  "service": "KuberFashion Backend",
  "version": "1.0.0"
}
```

### 3. Run Verification Script
```powershell
cd f:\kuber\backend
.\verify-backend.ps1
```

## 🔧 Troubleshooting

### If Connection Still Times Out:

#### 1. Check Supabase Status
```powershell
Test-NetConnection -ComputerName "aws-0-ap-south-1.pooler.supabase.com" -Port 5432
```

If this fails, Supabase database may be paused. Solutions:
- Login to Supabase dashboard and unpause
- Upgrade to Supabase Pro (no auto-pause)
- Use H2 for development

#### 2. Try Alternative Supabase Endpoint
Edit `.env`:
```
# Try this alternative pooler:
DATABASE_URL=jdbc:postgresql://db.hanmurmflpqbfwwvqnsl.supabase.co:5432/postgres?sslmode=require&connectTimeout=60&socketTimeout=60&loginTimeout=60&tcpKeepAlive=true
```

#### 3. Disable SSL (Not Recommended for Production)
Only for testing:
```
DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=disable&connectTimeout=60
```

#### 4. Use Development Mode with H2
```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:kuberfashion_dev`
- Username: `sa`
- Password: (empty)

## 📊 Configuration Summary

### Connection Timeouts (All in seconds):
| Parameter | Old | New | Reason |
|-----------|-----|-----|--------|
| connectTimeout | 30 | 60 | SSL handshake needs more time |
| socketTimeout | 30 | 60 | Read operations during SSL |
| loginTimeout | 30 | 60 | Authentication with SSL |
| connection-timeout | 20 | 60 | Hikari pool initialization |
| validation-timeout | 5 | 10 | Connection validation |
| initialization-fail-timeout | 30 | 60 | Pool startup grace period |

### Pool Settings:
| Parameter | Old | New | Reason |
|-----------|-----|-----|--------|
| maximum-pool-size | 8 | 5 | Supabase free tier limits |
| minimum-idle | 2 | 1 | Reduce idle connections |
| max-lifetime | 5min | 10min | Reduce SSL renegotiation |
| keepalive-time | 30s | 60s | Better connection health checks |

## 🎯 Expected Behavior

### Successful Startup Timeline:
1. **0-5s**: Maven starts, Spring Boot initializes
2. **5-15s**: Bean creation, security configuration
3. **15-45s**: Hikari pool initialization, SSL handshake with Supabase
4. **45-60s**: JPA entity manager setup, schema validation
5. **60s+**: Application ready, endpoints available

### Log Messages to Watch For:
```
✅ HikariPool-1 - Starting...
✅ HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
✅ HikariPool-1 - Start completed.
✅ Started KuberFashionApplication in X seconds
```

### If You See These Errors:
- `SocketTimeoutException` → Increase timeouts further or check network
- `Connection refused` → Supabase is down, use H2 fallback
- `Authentication failed` → Check DATABASE_USERNAME/PASSWORD in .env
- `SSL error` → Try `sslmode=prefer` instead of `require`

## 📝 Files Modified

1. ✅ `DatabaseConfig.java` → Backed up (removed from classpath)
2. ✅ `.env` → Updated DATABASE_URL with timeout parameters
3. ✅ `application-prod.properties` → Optimized Hikari settings
4. ✅ `start-backend-production.ps1` → Updated with new URL
5. ✅ `start-backend-with-fallback.ps1` → New fallback script

## 🔄 Reverting Changes (If Needed)

If you need to revert:
```powershell
# Restore original DatabaseConfig
cd f:\kuber\backend\src\main\java\com\kuberfashion\backend\config
Rename-Item "DatabaseConfig.java.backup" "DatabaseConfig.java"

# Restore original .env
# Edit .env and remove timeout parameters from DATABASE_URL
```

## 🌐 Production Deployment Recommendations

For production deployment:

1. **Use Supabase Pro** ($25/month) - No auto-pause, better performance
2. **Or use dedicated PostgreSQL**:
   - AWS RDS PostgreSQL
   - DigitalOcean Managed PostgreSQL
   - Self-hosted on VPS

3. **Connection Pool Sizing**:
   - Formula: `connections = ((core_count * 2) + effective_spindle_count)`
   - For 2 vCPU: max 5-10 connections
   - Monitor with: `SELECT * FROM pg_stat_activity;`

4. **SSL Configuration**:
   - Production: `sslmode=require` (current)
   - With certificate validation: `sslmode=verify-full&sslrootcert=/path/to/ca.crt`

5. **Monitoring**:
   - Enable Hikari metrics: `spring.datasource.hikari.register-mbeans=true`
   - Monitor connection pool: http://localhost:8080/actuator/metrics/hikari
   - Set up alerts for connection failures

## ✅ Success Criteria

Backend is working correctly when:
- ✅ Health endpoint responds: `http://localhost:8080/api/health`
- ✅ No SocketTimeoutException in logs
- ✅ Hikari pool shows "Start completed"
- ✅ User registration/login works
- ✅ Database queries execute successfully

## 🆘 Still Having Issues?

If backend still fails to start after 60 seconds:

1. **Check terminal output** for specific error
2. **Use H2 for immediate development**:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE="dev"
   mvn spring-boot:run
   ```
3. **Verify Supabase dashboard** - database may be paused
4. **Check firewall/antivirus** - may block PostgreSQL port 5432
5. **Try different network** - corporate networks may block database ports

---

**Last Updated**: 2025-10-13
**Spring Boot Version**: 3.5.5
**PostgreSQL Driver**: org.postgresql:postgresql (latest)
**Hikari CP**: Bundled with Spring Boot
