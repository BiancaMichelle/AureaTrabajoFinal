#!/bin/bash
cd /mnt/c/Users/NicolasSosa/Desktop/AureaTrabajoFinal/jitsi-meet

echo "Corrigiendo config.js de Jitsi..."

sudo docker compose exec web sed -i 's|https://localhost:8000/|http://localhost:8000/|g' /config/config.js
sudo docker compose exec web sed -i 's|wss://localhost:8000/|ws://localhost:8000/|g' /config/config.js

echo "✅ Correcciones aplicadas"
echo ""
echo "Verificando configuración:"
sudo docker compose exec web grep -E 'config\.(bosh|websocket)' /config/config.js
