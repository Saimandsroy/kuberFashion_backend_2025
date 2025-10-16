#!/bin/bash
# Run KuberFashion Backend in Development Mode with H2 Database

echo "🚀 Starting KuberFashion Backend (Development Mode)"
echo "📦 Using H2 in-memory database"
echo "🌐 Server will be available at: http://localhost:8080"
echo "🔍 H2 Console: http://localhost:8080/h2-console"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=dev
