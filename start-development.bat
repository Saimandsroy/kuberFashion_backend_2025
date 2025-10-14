@echo off
echo ========================================
echo  KuberFashion Backend - Development Start
echo ========================================

echo.
echo Setting development environment variables...
set SPRING_PROFILES_ACTIVE=dev
set SERVER_PORT=8080

echo.
echo Starting application in development mode with H2 database...
echo H2 Console will be available at: http://localhost:8080/h2-console
echo JDBC URL: jdbc:h2:mem:kuberfashion_dev
echo Username: sa
echo Password: (empty)

echo.
mvn spring-boot:run -Dspring-boot.run.profiles=dev

pause
