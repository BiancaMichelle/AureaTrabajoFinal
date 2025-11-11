# 🚀 Integración de Mercado Pago - Documentación

## ✅ Implementación Completada

### 1. **Dependencias Instaladas**
- ✅ SDK de Mercado Pago v2.5.0 en `pom.xml`

### 2. **Configuración**
Las credenciales están configuradas en `application.properties`:
```properties
mercadopago.access.token=TEST-3103555751406143-111023-ef048da9ac4254a7713349f263214d9e-2074311548
mercadopago.public.key=TEST-2ae34273-34da-497d-8f84-978bd90d25d9
```

### 3. **Archivos Creados/Modificados**

#### Nuevos Archivos:
1. **`MercadoPagoService.java`** - Servicio principal
   - `crearPreferenciaPago()` - Crea preferencia de pago
   - `procesarNotificacionPago()` - Procesa webhooks de MP
   - `crearInscripcion()` - Activa inscripción tras pago aprobado

2. **`MercadoPagoController.java`** - Controlador de endpoints
   - `POST /pago/webhook` - Recibe notificaciones de Mercado Pago
   - `GET /pago/success` - Página de pago exitoso
   - `GET /pago/pending` - Página de pago pendiente
   - `GET /pago/failure` - Página de pago fallido

3. **`pago-resultado.html`** - Vista de resultado del pago

#### Archivos Modificados:
1. **`Pago.java`** - Modelo extendido con campos de Mercado Pago
   - `preferenceId`, `paymentId`, `merchantOrderId`
   - `tipoPago`, `emailPagador`, `nombrePagador`
   - `externalReference`, `comprobanteEnviado`, `esCuotaMensual`

2. **`PagoRepository.java`** - Métodos para Mercado Pago
   - `findByPreferenceId()`
   - `findByPaymentId()`
   - `findByExternalReference()`

3. **`AlumnoController.java`** - Flujo de inscripción modificado
   - Ahora redirige a Mercado Pago en vez de inscribir directamente
   - Usa `MercadoPagoService` para crear preferencia

4. **`SecurityConfig.java`** - Permisos actualizados
   - `/pago/**` permitido para todos
   - `/pago/webhook` excluido de CSRF

5. **`InscripcionRepository.java`** - Nuevo método
   - `findByAlumnoDniAndOfertaId()` para buscar inscripción específica

---

## 🔄 Flujo de Inscripción con Pago

### 1. **Usuario se inscribe a un curso**
```
Usuario → Click "Inscribirse" → AlumnoController.inscribirseAOferta()
```

### 2. **Creación de preferencia de pago**
```java
// AlumnoController llama a:
String urlPago = mercadoPagoService.crearPreferenciaPago(usuario, oferta);
// Redirige a Mercado Pago:
return "redirect:" + urlPago;
```

### 3. **Usuario realiza el pago en Mercado Pago**
- El usuario es redirigido a la pasarela de Mercado Pago
- Ingresa los datos de su tarjeta/método de pago
- Mercado Pago procesa el pago

### 4. **Mercado Pago notifica el resultado**

#### A) **Via Webhook** (Automático - Recomendado)
```
Mercado Pago → POST /pago/webhook → MercadoPagoController
                                   ↓
                          MercadoPagoService.procesarNotificacionPago()
                                   ↓
                          Si está APROBADO → Crea Inscripción
```

#### B) **Via Redirect** (Cuando el usuario vuelve)
```
Usuario vuelve → GET /pago/success?payment_id=XXX
                            ↓
                MercadoPagoController.pagoExitoso()
                            ↓
                Muestra página de confirmación
```

### 5. **Inscripción activada**
```sql
INSERT INTO inscripciones (alumno_id, oferta_id, estado, fecha)
VALUES (usuario, oferta, true, NOW());
```

---

## 📋 Estados de Pago

| Estado MP | Estado DB | Acción |
|-----------|-----------|--------|
| `approved` | `COMPLETADO` | ✅ Crear inscripción |
| `pending` | `PENDIENTE` | ⏳ Esperar confirmación |
| `in_process` | `PENDIENTE` | ⏳ Esperar confirmación |
| `rejected` | `RECHAZADO` | ❌ No inscribir |
| `cancelled` | `RECHAZADO` | ❌ No inscribir |
| `refunded` | `RECHAZADO` | 💸 Desactivar inscripción |

---

## 🔗 URLs de la Aplicación

### Desarrollo (localhost):
- Success: `http://localhost:8080/pago/success`
- Pending: `http://localhost:8080/pago/pending`
- Failure: `http://localhost:8080/pago/failure`
- Webhook: `http://localhost:8080/pago/webhook`

### Producción (cuando despliegues):
Deberás cambiar las URLs en `MercadoPagoService.java`:
```java
PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
    .success("https://tudominio.com/pago/success")
    .failure("https://tudominio.com/pago/failure")
    .pending("https://tudominio.com/pago/pending")
    .build();
```

---

## 🔔 Configurar Webhooks en Producción

### 1. **Ingresar a Mercado Pago**
- Ve a: https://www.mercadopago.com.ar/developers/panel
- Selecciona tu aplicación

### 2. **Configurar Webhooks**
- Ve a la sección "Webhooks"
- Agregar nueva URL: `https://tudominio.com/pago/webhook`
- Seleccionar eventos:
  - ✅ `payment` (Pagos)
  - ✅ `merchant_order` (Órdenes)

### 3. **Verificar Webhook**
Mercado Pago enviará un POST de prueba:
```json
{
  "action": "payment.created",
  "api_version": "v1",
  "data": {
    "id": "123456789"
  },
  "date_created": "2024-01-01T00:00:00Z",
  "id": 12345,
  "live_mode": true,
  "type": "payment",
  "user_id": "123456"
}
```

---

## 🧪 Pruebas con Tarjetas de Prueba

### Tarjetas de Crédito de Prueba:

| Tarjeta | Número | Código | Fecha |
|---------|--------|--------|-------|
| **Visa Aprobada** | 4509 9535 6623 3704 | 123 | 11/25 |
| **Mastercard Aprobada** | 5031 7557 3453 0604 | 123 | 11/25 |
| **Rechazada (fondos)** | 4002 7081 9439 3404 | 123 | 11/25 |

### Datos del titular:
- Nombre: APRO (para aprobado) o OTHE (para rechazado)
- DNI: 12345678
- Email: test_user_XXXXXXX@testuser.com

---

## 📊 Base de Datos

### Tabla `pagos`:
```sql
- id_pago (PK)
- usuario_id (FK)
- id_oferta (FK)
- id_inscripcion (FK)
- preference_id (Mercado Pago)
- payment_id (Mercado Pago)
- merchant_order_id
- estado_pago (PENDIENTE, COMPLETADO, RECHAZADO)
- tipo_pago (credit_card, debit_card, etc.)
- monto
- email_pagador
- nombre_pagador
- fecha_pago
- fecha_aprobacion
- external_reference (único)
- comprobante_enviado (boolean)
- es_cuota_mensual (boolean)
- numero_cuota
```

---

## 📧 Próximos Pasos (TODO)

### 1. **Envío de Comprobantes por Email** 
```java
// En MercadoPagoService.crearInscripcion()
if (pago.getEstadoPago() == EstadoPago.COMPLETADO && !pago.getComprobanteEnviado()) {
    emailService.enviarComprobantePago(pago);
    pago.setComprobanteEnviado(true);
}
```

### 2. **Suscripciones Mensuales**
Implementar un `@Scheduled` que:
- Se ejecute el día 1 de cada mes
- Busque inscripciones activas
- Cree pagos mensuales automáticos
- Use la API de Suscripciones de Mercado Pago

```java
@Scheduled(cron = "0 0 0 1 * ?") // Día 1 de cada mes a las 00:00
public void procesarCuotasMensuales() {
    List<Inscripciones> inscripcionesActivas = inscripcionRepository.findByEstadoInscripcionTrue();
    // Crear preferencia de pago para cada una
}
```

### 3. **Panel de Administración de Pagos**
- Vista para ver todos los pagos
- Filtros por estado
- Reenvío de comprobantes
- Reembolsos manuales

---

## 🐛 Debugging

### Ver logs de Mercado Pago:
```bash
# En consola verás:
💳 Preferencia creada: XXXXX-XXXXX-XXXXX
🔗 Init Point: https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=XXXXX
📬 Notificación recibida para pago: 123456
📊 Estado: approved
✅ Pago aprobado - Inscripción creada
```

### Verificar en Base de Datos:
```sql
SELECT * FROM pagos ORDER BY fecha_pago DESC LIMIT 10;
SELECT * FROM inscripciones WHERE estado_inscripcion = true;
```

---

## 🔐 Seguridad

1. ✅ **Webhook sin CSRF** - `/pago/webhook` excluido de CSRF
2. ✅ **Validación de external_reference** - Evita duplicados
3. ✅ **Verificación del payment_id** - Solo pagos reales de MP
4. ⚠️ **TODO**: Validar firma de webhook en producción

---

## 📞 Soporte

- Documentación oficial: https://www.mercadopago.com.ar/developers/es/docs
- API Reference: https://www.mercadopago.com.ar/developers/es/reference
- Soporte: https://www.mercadopago.com.ar/developers/es/support

---

**✅ Integración completada y lista para pruebas!**
