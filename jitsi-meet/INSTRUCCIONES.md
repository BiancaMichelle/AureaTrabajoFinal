# 🎥 Jitsi Meet - Instrucciones de Uso

## ⚡ Inicio Rápido

### 1️⃣ INICIAR JITSI (cada vez que enciendas la PC)

Abre PowerShell y ejecuta:

```powershell
cd C:\Users\NicolasSosa\Desktop\AureaTrabajoFinal\jitsi-meet
wsl bash iniciar-jitsi.sh
```

**Ingresa tu contraseña de WSL cuando te la pida.**

Espera a que aparezca:
```
✅ Jitsi Meet iniciado correctamente
STATUS
jitsi-meet-web-1      Up
jitsi-meet-prosody-1  Up
jitsi-meet-jicofo-1   Up
jitsi-meet-jvb-1      Up
```

### 2️⃣ CORREGIR CONFIGURACIÓN (IMPORTANTE - Hazlo cada vez después de iniciar)

```powershell
wsl bash corregir-config.sh
```

Ingresa tu contraseña nuevamente. Debes ver:
```
✅ Correcciones aplicadas
config.bosh = 'http://localhost:8000/' + subdir + 'http-bind';
config.websocket = 'ws://localhost:8000/' + subdir + 'xmpp-websocket';
```

### 3️⃣ USAR TU APLICACIÓN

Ahora sí, inicia tu aplicación Spring Boot y crea clases normalmente.

---

## 🛑 Detener Jitsi (al terminar)

```powershell
cd C:\Users\NicolasSosa\Desktop\AureaTrabajoFinal\jitsi-meet
wsl bash detener-jitsi.sh
```

---

## ❓ Problemas Comunes

### "Reconectando..." o "Desconectado"
→ Ejecuta el script `corregir-config.sh` nuevamente

### "Permission denied" al ejecutar comandos
→ Asegúrate de estar ejecutando los comandos desde PowerShell, no desde WSL directamente

### Los contenedores no inician
→ Verifica que Docker Desktop esté corriendo en WSL

---

## 📝 Resumen

**Cada vez que uses Jitsi:**
1. `wsl bash iniciar-jitsi.sh` (inicia los contenedores)
2. `wsl bash corregir-config.sh` (corrige la configuración)
3. Inicia tu aplicación Spring Boot
4. Al terminar: `wsl bash detener-jitsi.sh`

---

## 🔧 Configuración

- **URL de Jitsi:** http://localhost:8000
- **Puerto HTTP:** 8000
- **Puerto UDP (video):** 10000
- **Autenticación:** Deshabilitada (cualquiera puede crear salas)
- **HTTPS:** Deshabilitado (solo HTTP para desarrollo local)

---

**Ubicación de archivos importantes:**
- Configuración: `.env`
- Docker Compose: `docker-compose.yml`
- Scripts: `iniciar-jitsi.sh`, `corregir-config.sh`, `detener-jitsi.sh`
