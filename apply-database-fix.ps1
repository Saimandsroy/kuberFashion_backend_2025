# Apply Database Schema Fix
Write-Host "=== Applying Database Schema Fix ===" -ForegroundColor Cyan

Write-Host "`n📋 Steps to fix the kuber_coupons column issue:" -ForegroundColor Yellow
Write-Host "1. Connect to your Supabase database" -ForegroundColor White
Write-Host "2. Execute the SQL command to add the missing column" -ForegroundColor White
Write-Host "3. Restart the backend application" -ForegroundColor White

Write-Host "`n🔧 SQL Command to execute in Supabase:" -ForegroundColor Yellow
Write-Host "ALTER TABLE users ADD COLUMN kuber_coupons INTEGER DEFAULT 0 NOT NULL;" -ForegroundColor Green

Write-Host "`n📝 Instructions:" -ForegroundColor Yellow
Write-Host "1. Go to https://supabase.com/dashboard" -ForegroundColor White
Write-Host "2. Select your project: hanmurmflpqbfwwvqnsl" -ForegroundColor White
Write-Host "3. Go to SQL Editor" -ForegroundColor White
Write-Host "4. Run the SQL command above" -ForegroundColor White
Write-Host "5. Verify with: SELECT * FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'kuber_coupons';" -ForegroundColor White

Write-Host "`n⚠️  Alternative: Use psql command line:" -ForegroundColor Yellow
Write-Host "psql 'postgresql://postgres.hanmurmflpqbfwwvqnsl:saimandsroy2005@@aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require'" -ForegroundColor Cyan

Write-Host "`n🔄 After applying the fix:" -ForegroundColor Yellow
Write-Host "1. Restart the backend: Ctrl+C then mvn spring-boot:run" -ForegroundColor White
Write-Host "2. Test user registration/login" -ForegroundColor White
Write-Host "3. Verify no more 'column does not exist' errors" -ForegroundColor White

Write-Host "`n✅ This will resolve the error permanently!" -ForegroundColor Green
