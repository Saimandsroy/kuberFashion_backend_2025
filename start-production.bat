@echo off
echo ========================================
echo  KuberFashion Backend - Production Start
echo ========================================

echo.
echo Setting production environment variables...
set SPRING_PROFILES_ACTIVE=prod
set DATABASE_URL=jdbc:postgresql://db.hanmurmflpqbfwwvqnsl.supabase.co:5432/postgres?sslmode=require
set DATABASE_USERNAME=postgres.hanmurmflpqbfwwvqnsl
set DATABASE_PASSWORD=saimandsroy2005@
set JWT_SECRET=u5PS40PD1gBpFFAgD7ugXd6k9klL+h9YZMv3gIjKH3Nof/qaJf2rPbr0wrwwe6WaDfq7q3JOjwdBF/6AaKz7sQ==
set SERVER_PORT=8080

echo.
echo Building application...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo ✅ Build successful!
echo Starting application in production mode...

java -jar target\backend-0.0.1-SNAPSHOT.jar ^
    --spring.profiles.active=prod ^
    --server.port=8080 ^
    --logging.level.com.kuberfashion.backend=INFO

pause
