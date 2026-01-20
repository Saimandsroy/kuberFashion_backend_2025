# Local Development Guide

## Running the Backend Locally

The backend has two profiles:
- **`dev`** - Uses H2 in-memory database (for local development)
- **`prod`** - Uses PostgreSQL (for production deployment)

### Quick Start (Development Mode)

```bash
# Option 1: Use the convenience script
./run-dev.sh

# Option 2: Run directly with Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Option 3: Set environment variable
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

### Verify Backend is Running

```bash
# Check health endpoint
curl http://localhost:8080/api/health

# Check products endpoint
curl http://localhost:8080/api/products
```

### Access H2 Console (Dev Mode Only)

When running in dev mode, you can access the H2 database console:

1. Open browser: http://localhost:8080/h2-console
2. Use these settings:
   - **JDBC URL**: `jdbc:h2:mem:kuberfashion_dev`
   - **Username**: `sa`
   - **Password**: (leave empty)

### Configuration Files

- `application.properties` - Base configuration (defaults to prod profile)
- `application-dev.properties` - Development overrides (H2, debug logging)
- `application-prod.properties` - Production overrides (PostgreSQL, optimized settings)

### Key Differences: Dev vs Prod

| Feature | Dev Profile | Prod Profile |
|---------|------------|--------------|
| Database | H2 (in-memory) | PostgreSQL |
| DDL Auto | create-drop | update |
| Flyway | Disabled | Enabled |
| Logging | DEBUG | INFO |
| CORS | localhost only | Production domains |
| Upload Limits | 10MB | 20MB |

### Troubleshooting

**Error: Connection to localhost:5432 refused**
- You're running with prod profile by default
- Solution: Use `-Dspring-boot.run.profiles=dev` flag

**Error: Could not resolve placeholder 'cloudflare.r2.access-key'**
- Missing Cloudflare R2 configuration
- Solution: Already fixed in `application-dev.properties` with dummy values

**Error: Circular depends-on relationship between 'flyway' and 'entityManagerFactory'**
- Flyway conflicts with JPA in dev mode
- Solution: Already fixed by disabling Flyway in dev profile

### Environment Variables (Optional)

For dev mode, you can optionally set:

```bash
# Supabase service key (for image uploads)
export SUPABASE_SERVICE_KEY=your-service-key-here

# Custom port
export SERVER_PORT=8080

# Custom database URL (if not using H2)
export DATABASE_URL=jdbc:postgresql://localhost:5432/kuberfashion
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=yourpassword
```

### Production Deployment

For production, ensure these environment variables are set on your server:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/kuberfashion
export DATABASE_USERNAME=your-db-user
export DATABASE_PASSWORD=your-db-password
export SUPABASE_SERVICE_KEY=your-supabase-service-key
export JWT_SECRET=your-secure-jwt-secret
```

Then build and run:

```bash
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```
