#!/bin/bash
# CloudVault Backend Startup Script
cd "$(dirname "$0")/backend" || exit 1

# Export env vars from .env file
if [ -f .env ]; then
  export $(cat .env | grep -v '^#' | xargs)
else
  echo "Warning: .env file not found. Falling back to default environment variables or system vars."
fi

echo "Starting CloudVault backend..."
echo "DB_HOST=$DB_HOST"
echo "DB_PORT=$DB_PORT"
echo "DATABASE_USER=$DATABASE_USER"

/tmp/apache-maven-3.9.6/bin/mvn spring-boot:run -DskipTests
