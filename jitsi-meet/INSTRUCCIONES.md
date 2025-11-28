# 🎥 Jitsi Meet - Instrucciones de Uso

## ⚡ Inicio Rápido

### 1️⃣ INICIAR JITSI (cada vez que enciendas la PC)

Abre PowerShell y ejecuta:

```powershell
cd "C:\Users\HDC i5 10400\Desktop\AureaTrabajoFinal\jitsi-meet"
wsl bash iniciar-jitsi.sh
```

**Ingresa tu contraseña de WSL cuando te la pida.**

Espera a que aparezca:
```
✅ Jitsi Meet iniciado correctamente
```

### 2️⃣ INICIAR TÚNEL (NGROK)

Para que las reuniones funcionen externamente, necesitas exponer el puerto 8100.

1. Abre una nueva terminal PowerShell.
2. Ejecuta:
   ```powershell
   ngrok http 8100
   ```
3. Copia la URL HTTPS que te da ngrok (ej: `https://xxxx-xxxx.ngrok-free.app`).

### 3️⃣ ACTUALIZAR CONFIGURACIÓN (Si cambió la URL)

Si la URL de ngrok cambió, debes actualizarla en dos archivos:

1. **En `jitsi-meet/.env`:**
   ```properties
   PUBLIC_URL=https://tu-nueva-url.ngrok-free.app
   ```
   *(Luego reinicia Jitsi con `wsl bash iniciar-jitsi.sh`)*

2. **En `demo/src/main/resources/application.properties`:**
   ```properties
   jitsi.meet.url=https://tu-nueva-url.ngrok-free.app
   ```
   *(Luego reinicia tu aplicación Spring Boot)*

---

## 🛑 Detener Jitsi

Cuando termines de usarlo, para liberar recursos:

```powershell
cd "C:\Users\HDC i5 10400\Desktop\AureaTrabajoFinal\jitsi-meet"
wsl bash detener-jitsi.sh
```

---

## ⚙️ Configuración Técnica

- **Puerto HTTP Jitsi:** 8100 (Interno)
- **Puerto HTTPS Jitsi:** 8444 (Interno - Deshabilitado para evitar conflictos)
- **Puerto UDP (Video):** 10000
- **Autenticación:** Deshabilitada (cualquiera puede crear salas)

---

## ❓ Problemas Comunes

### "ERR_NGROK_8012" o "Connection refused"
→ Significa que Jitsi no está corriendo. Ejecuta el paso 1 de nuevo.

### "Reconectando..." en la videollamada
→ Verifica que la URL en `.env` coincida exactamente con la de ngrok.

### "Permission denied" al ejecutar comandos
→ Asegúrate de estar ejecutando los comandos desde PowerShell, no desde WSL directamente.

---

**Ubicación de archivos importantes:**
- Configuración: `.env`
- Docker Compose: `docker-compose.yml`
- Scripts: `iniciar-jitsi.sh`, `detener-jitsi.sh`
