# Quick Fix Guide for Backend Issues

## Issues Found:
1. ❌ Duplicate Flyway migrations (V1 and V2 duplicates)
2. ❌ Missing `kuber_coupons` column in database
3. ❌ Flyway validation failing

## Solution Applied:

### Step 1: Disabled Flyway Temporarily
- Set `spring.flyway.enabled=false` in `application-prod.properties`
- This prevents migration conflicts

### Step 2: Changed Hibernate DDL Mode
- Set `spring.jpa.hibernate.ddl-auto=update` temporarily
- This will auto-create missing columns like `kuber_coupons`

### Step 3: Removed Duplicate Migration
- Deleted `V1__initial_schema.sql` (kept `V1__Create_users_table.sql`)
- Kept existing V2 and V3 migrations

## How to Start Backend:

### Option A: Quick Start (Recommended)
```powershell
cd f:\kuber\backend
mvn spring-boot:run
```

The backend will:
- Connect to Supabase PostgreSQL
- Auto-create missing `kuber_coupons` column
- Start on port 8080

### Option B: Build JAR and Run
```powershell
cd f:\kuber\backend
mvn clean package -DskipTests
java -jar target\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## After Backend Starts Successfully:

### 1. Test Health Endpoint
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
```

### 2. Test User Registration
```powershell
$testUser = @{
    firstName = "Test"
    lastName = "User"
    email = "test@example.com"
    phone = "9876543210"
    password = "Test@123"
    referralCode = "kuberfashion2025"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body $testUser
```

### 3. Test Phone Login
```powershell
$loginData = @{
    phone = "9876543210"
    password = "Test@123"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/phone/login" -Method POST -ContentType "application/json" -Body $loginData
```

## After Verification (Optional - Production Best Practice):

Once everything works, you can switch back to stricter settings:

1. Change `spring.jpa.hibernate.ddl-auto=validate` in `application-prod.properties`
2. Enable Flyway: `spring.flyway.enabled=true`
3. Clean up migration files to avoid duplicates

## Current Configuration:
- Database: PostgreSQL (Supabase) at `aws-0-ap-south-1.pooler.supabase.com`
- Profile: Production (`SPRING_PROFILES_ACTIVE=prod`)
- Port: 8080
- CORS: Configured for network access (10.*.*.*, 172.*.*.*, 192.*.*.*)

## Troubleshooting:

### If backend still fails to start:
1. Check terminal output for specific errors
2. Verify Supabase database is accessible:
   ```powershell
   Test-NetConnection -ComputerName "aws-0-ap-south-1.pooler.supabase.com" -Port 5432
   ```
3. Check `.env` file has correct credentials

### If "column does not exist" error persists:
The `ddl-auto=update` setting should auto-create it, but if not, manually run:
```sql
ALTER TABLE users ADD COLUMN kuber_coupons INTEGER DEFAULT 0 NOT NULL;
```

## Files Modified:
- `application-prod.properties` - Disabled Flyway, set ddl-auto=update
- `db/migration/` - Removed duplicate V1__initial_schema.sql

## Next Steps:
1. Start backend with `mvn spring-boot:run`
2. Test all endpoints
3. Verify frontend can connect
4. Monitor logs for any errors
