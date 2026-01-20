#!/bin/bash
# Run KuberFashion Backend in Development Mode with PostgreSQL Database

echo "🚀 Starting KuberFashion Backend (Development Mode)"
echo "📦 Using PostgreSQL database"
echo "🌐 Server will be available at: http://localhost:8080"
echo ""

# Load environment variables if .env exists
if [ -f .env ]; then
  echo "📄 Loading environment variables from .env..."
  export $(grep -v '^#' .env | xargs)
fi

mvn spring-boot:run -Dspring-boot.run.profiles=dev
