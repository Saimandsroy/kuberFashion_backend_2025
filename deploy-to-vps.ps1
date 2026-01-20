# Deploy KuberFashion to VPS
Write-Host "=== KuberFashion VPS Deployment Guide ===" -ForegroundColor Cyan

Write-Host "`nThis script will help you deploy KuberFashion to your VPS." -ForegroundColor Yellow
Write-Host "Make sure you have:" -ForegroundColor White
Write-Host "  ✅ VPS with Ubuntu 20.04+ or similar" -ForegroundColor Green
Write-Host "  ✅ SSH access to your VPS" -ForegroundColor Green
Write-Host "  ✅ Domain name pointing to your VPS" -ForegroundColor Green
Write-Host "  ✅ Docker installed on VPS" -ForegroundColor Green

$vpsHost = Read-Host "`nEnter your VPS IP address or domain"
$sshUser = Read-Host "Enter SSH username (usually 'root' or 'ubuntu')"
$domain = Read-Host "Enter your domain name (e.g., yourdomain.com)"

Write-Host "`n=== Step 1: Prepare Local Files ===" -ForegroundColor Cyan

# Build the application
Write-Host "Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "FAIL Build failed. Fix errors and try again." -ForegroundColor Red
    exit 1
}

Write-Host "OK Application built successfully" -ForegroundColor Green

# Create deployment package
Write-Host "Creating deployment package..." -ForegroundColor Yellow
$deployDir = "deploy-package"
if (Test-Path $deployDir) {
    Remove-Item $deployDir -Recurse -Force
}
New-Item -ItemType Directory -Path $deployDir | Out-Null

# Copy necessary files
Copy-Item "target/*.jar" "$deployDir/"
Copy-Item "docker-compose.yml" "$deployDir/"
Copy-Item "docker-compose.prod.yml" "$deployDir/"
Copy-Item ".env.vps" "$deployDir/.env"
Copy-Item "init-scripts" "$deployDir/" -Recurse

Write-Host "OK Deployment package created" -ForegroundColor Green

Write-Host "`n=== Step 2: VPS Commands ===" -ForegroundColor Cyan
Write-Host "Execute these commands on your VPS:" -ForegroundColor Yellow

$commands = @"

# 1. Update system
sudo apt update && sudo apt upgrade -y

# 2. Install Docker and Docker Compose
sudo apt install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $sshUser

# 3. Install Java 21
sudo apt install -y openjdk-21-jdk

# 4. Install Nginx
sudo apt install -y nginx

# 5. Install Certbot for SSL
sudo apt install -y certbot python3-certbot-nginx

# 6. Create application directory
sudo mkdir -p /opt/kuberfashion
sudo chown $sshUser:$sshUser /opt/kuberfashion

# 7. Upload files (run this from your local machine):
scp -r $deployDir/* $sshUser@${vpsHost}:/opt/kuberfashion/

# 8. On VPS, navigate to app directory
cd /opt/kuberfashion

# 9. Update environment variables in .env file
nano .env
# Update:
# - DATABASE_PASSWORD (use a strong password)
# - JWT_SECRET (generate a new long random string)
# - CORS_ALLOWED_ORIGINS (your domain)
# - CLOUDFLARE_R2_* (your R2 credentials)

# 10. Start PostgreSQL
docker-compose up -d postgres

# 11. Wait for PostgreSQL to be ready
sleep 30

# 12. Start the application
nohup java -jar *.jar --spring.profiles.active=prod > app.log 2>&1 &

# 13. Configure Nginx
sudo nano /etc/nginx/sites-available/kuberfashion

# Add this configuration:
server {
    listen 80;
    server_name $domain www.$domain;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

# 14. Enable the site
sudo ln -s /etc/nginx/sites-available/kuberfashion /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx

# 15. Get SSL certificate
sudo certbot --nginx -d $domain -d www.$domain

# 16. Create systemd service for auto-restart
sudo nano /etc/systemd/system/kuberfashion.service

# Add this content:
[Unit]
Description=KuberFashion Backend
After=network.target

[Service]
Type=simple
User=$sshUser
WorkingDirectory=/opt/kuberfashion
ExecStart=/usr/bin/java -jar /opt/kuberfashion/*.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target

# 17. Enable and start the service
sudo systemctl daemon-reload
sudo systemctl enable kuberfashion
sudo systemctl start kuberfashion

# 18. Check status
sudo systemctl status kuberfashion
sudo systemctl status nginx
docker-compose ps

"@

Write-Host $commands -ForegroundColor White

Write-Host "`n=== Step 3: Frontend Deployment ===" -ForegroundColor Cyan
Write-Host "For frontend deployment:" -ForegroundColor Yellow

$frontendCommands = @"

# Option A: Deploy to Cloudflare Pages
1. Build frontend: npm run build
2. Upload dist/ folder to Cloudflare Pages
3. Set environment variable: VITE_API_URL=https://$domain

# Option B: Deploy to same VPS
1. Copy frontend build to VPS: /var/www/html/
2. Update Nginx to serve frontend files
3. Configure API proxy as shown above

"@

Write-Host $frontendCommands -ForegroundColor White

Write-Host "`n=== Step 4: Verification ===" -ForegroundColor Cyan
Write-Host "After deployment, verify:" -ForegroundColor Yellow

$verificationSteps = @"

1. Backend health: curl https://$domain/api/health
2. User registration: Test on frontend
3. Admin login: Test admin panel
4. File upload: Test image uploads
5. Database: Check PgAdmin at https://pgadmin.$domain
6. SSL certificate: Check https://$domain
7. Logs: tail -f /opt/kuberfashion/app.log

"@

Write-Host $verificationSteps -ForegroundColor White

Write-Host "`n=== Deployment Package Ready ===" -ForegroundColor Green
Write-Host "Files prepared in: $deployDir/" -ForegroundColor Cyan
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Execute VPS commands above" -ForegroundColor White
Write-Host "2. Upload files: scp -r $deployDir/* $sshUser@${vpsHost}:/opt/kuberfashion/" -ForegroundColor White
Write-Host "3. Configure environment variables" -ForegroundColor White
Write-Host "4. Start services" -ForegroundColor White
Write-Host "5. Configure Nginx and SSL" -ForegroundColor White

Write-Host "`n🚀 Your VPS deployment guide is ready!" -ForegroundColor Green
