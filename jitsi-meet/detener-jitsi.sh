#!/bin/bash
# Script para detener Jitsi Meet en Docker (WSL)
# Uso: ./detener-jitsi.sh

echo "═══════════════════════════════════════════════════════"
echo "   🛑 DETENIENDO JITSI MEET"
echo "═══════════════════════════════════════════════════════"
echo ""

# Obtener el directorio del script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

echo "¿Qué deseas hacer?"
echo ""
echo "1. Detener servicios (mantener configuración)"
echo "2. Detener y eliminar contenedores (mantener configuración)"
echo "3. Detener y ELIMINAR TODO (incluyendo configuración)"
echo ""
read -p "Opción (1/2/3): " OPCION
echo ""

case $OPCION in
    1)
        echo "🛑 Deteniendo servicios..."
        sudo docker compose stop
        echo ""
        echo "✅ Servicios detenidos"
        echo "💡 Para reiniciar: sudo docker compose start"
        ;;
    2)
        echo "🛑 Deteniendo y eliminando contenedores..."
        sudo docker compose down
        echo ""
        echo "✅ Contenedores eliminados (configuración preservada)"
        echo "💡 Para reiniciar: sudo docker compose up -d"
        ;;
    3)
        echo "⚠️  ADVERTENCIA: Esto eliminará TODA la configuración"
        read -p "¿Estás seguro? (S/N): " CONFIRMAR
        
        if [[ "$CONFIRMAR" =~ ^[Ss]$ ]]; then
            echo ""
            echo "🗑️  Eliminando todo..."
            sudo docker compose down -v
            
            if [ -d ".jitsi-meet-cfg" ]; then
                rm -rf .jitsi-meet-cfg
                echo "✅ Configuración eliminada"
            fi
            
            echo ""
            echo "✅ Todo eliminado completamente"
            echo "💡 Para volver a usar: sudo docker compose up -d"
        else
            echo "❌ Operación cancelada"
        fi
        ;;
    *)
        echo "❌ Opción inválida"
        ;;
esac

echo ""
