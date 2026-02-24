#!/bin/bash

set -e

echo "🚀 Starting rebuild and restart process..."

# Цвета для вывода
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Функция для вывода цветных сообщений
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверяем наличие docker-compose.yml
if [ ! -f "docker-compose.yml" ]; then
    log_error "docker-compose.yml not found in current directory!"
    exit 1
fi

# Останавливаем и удаляем контейнеры, если они работают
log_info "Stopping existing containers..."
docker-compose down --remove-orphans || true

# Собираем все JAR файлы
log_info "Building all JAR files..."
./gradlew clean build -x test

# Проверяем успешность сборки
if [ $? -ne 0 ]; then
    log_error "Build failed!"
    exit 1
fi

log_info "Build completed successfully!"

# Собираем и запускаем Docker образы
log_info "Building and starting Docker containers..."
docker-compose up --build -d

# Проверяем статус контейнеров
log_info "Checking container status..."
sleep 5
docker-compose ps

echo ""
log_info "✅ All services have been rebuilt and restarted!"
log_info "📊 Services available at:"
echo "   🤖 s21bot: http://localhost:8083/actuator/prometheus"
echo "   🔐 s21auth: http://localhost:8081/actuator/prometheus" 
echo "   🎓 s21edu: http://localhost:8082/actuator/prometheus"
echo "   🚀 s21rocket: http://localhost:8084/actuator/prometheus"
echo ""
log_info "📈 Monitoring stack:"
echo "   📊 Prometheus: http://localhost:9090"
echo "   📈 Grafana: http://localhost:3001 (admin/admin)"
echo ""
log_info "📋 Useful commands:"
echo "   📋 View logs: docker-compose logs -f [service_name]"
echo "   📋 View all logs: docker-compose logs -f"
echo "   ⚠️  Stop all services: docker-compose down"
echo "   🔄 Restart single service: docker-compose restart [service_name]"
