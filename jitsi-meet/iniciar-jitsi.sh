#!/bin/bash
# Script para levantar Jitsi Meet en Docker (WSL)
# Uso: ./iniciar-jitsi.sh

echo "═══════════════════════════════════════════════════════"
echo "   🎥 INICIANDO JITSI MEET EN DOCKER"
echo "═══════════════════════════════════════════════════════"
echo ""

# Verificar si Docker está instalado

DOCKER_VERSION=$(docker --version)
echo "   ✅ Docker encontrado: $DOCKER_VERSION"

# Verificar si Docker está corriendo
echo "🔍 Verificando que Docker esté activo..."
if ! docker ps &> /dev/null; then
    echo "   ❌ ERROR: Docker no está corriendo"
    echo "   👉 Inicia el servicio Docker con: sudo service docker start"
    exit 1
fi
echo "   ✅ Docker está activo"

# Obtener el directorio del script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo "📂 Ubicación: $SCRIPT_DIR"
echo ""

# Verificar archivos necesarios
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ ERROR: No se encuentra docker-compose.yml"
    exit 1
fi

if [ ! -f ".env" ]; then
    echo "❌ ERROR: No se encuentra .env"
    exit 1
fi

echo "✅ Archivos de configuración encontrados"
echo ""

# Preguntar si quiere ver los logs
read -p "¿Deseas ver los logs después de iniciar? (S/N): " VER_LOGS
echo ""

echo "🚀 Iniciando servicios de Jitsi Meet..."
echo "   (Esto puede tardar varios minutos la primera vez)"
echo ""

# Levantar los servicios (Docker Compose v2)
sudo docker compose up -d

if [ $? -eq 0 ]; then
    echo ""
    echo "═══════════════════════════════════════════════════════"
    echo "   ✅ JITSI MEET INICIADO CORRECTAMENTE"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "📍 Accede a Jitsi en: \e[33mhttps://tzvhc3m8-8000.brs.devtunnels.ms\e[0m (o http://localhost:8000)"
    echo ""
    echo "📊 Estado de los contenedores:"
    echo ""
    sudo docker compose ps
    echo ""
    
    # Contar servicios
    TOTAL_SERVICES=$(sudo docker compose ps --services | wc -l)
    RUNNING_SERVICES=$(sudo docker compose ps --services --filter "status=running" | wc -l)
    
    if [ "$TOTAL_SERVICES" -eq "$RUNNING_SERVICES" ]; then
        echo "✅ Todos los servicios están corriendo correctamente"
    else
        echo "⚠️  Algunos servicios no están corriendo. Revisa el estado arriba."
    fi
    
    echo ""
    echo "🎯 Próximos pasos:"
    echo "   1. Abre http://localhost:8000 en tu navegador"
    echo "   2. Reinicia tu aplicación Spring Boot"
    echo "   3. Crea una clase de videoconferencia"
    echo "   4. ¡Disfruta de reuniones sin límite de tiempo!"
    echo ""
    
    if [[ "$VER_LOGS" =~ ^[Ss]$ ]]; then
        echo "📋 Mostrando logs (presiona Ctrl+C para salir)..."
        echo ""
        sudo docker compose logs -f
    else
        echo "💡 Para ver los logs más tarde, ejecuta: sudo docker compose logs -f"
        echo "💡 Para detener Jitsi, ejecuta: sudo docker compose stop"
        echo ""
    fi
else
    echo ""
    echo "❌ ERROR al iniciar Jitsi Meet"
    echo "   Revisa los logs con: sudo docker compose logs"
    echo ""
    exit 1
fi
