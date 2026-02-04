# 🎓 Sistema de Certificación y Cierre de Notas

## 📋 Resumen Ejecutivo

Sistema completo para gestionar el cierre de calificaciones y emisión de certificados al finalizar ofertas académicas.

### Flujo General

```
┌─────────────────────────────────────────────────────────┐
│  FASE 1: Finalización de Oferta                        │
│  ├─> Docente/Admin marca oferta como FINALIZADA        │
│  └─> Sistema ejecuta cálculo automático                │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  FASE 2: Propuesta Automática                          │
│  ├─> Sistema analiza cada inscripción:                 │
│  │   • Promedio ≥ 7.0                                  │
│  │   • Asistencia ≥ 75%                                │
│  │   • Tareas entregadas ≥ 80%                         │
│  │   • Exámenes aprobados = 100%                       │
│  └─> Genera lista PROPUESTA para certificación         │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  FASE 3: Revisión Docente                              │
│  Ubicación: /aula/oferta/{id}/certificaciones          │
│  ├─> Ver tabla con alumnos propuestos                  │
│  ├─> Ver métricas detalladas (promedio, asistencia)    │
│  ├─> Agregar alumnos manualmente (casos especiales)    │
│  ├─> Quitar alumnos de la lista                        │
│  └─> Agregar observaciones por alumno                  │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  FASE 4: Cierre Final (IRREVERSIBLE)                   │
│  ├─> Docente presiona "Cerrar Notas y Emitir"          │
│  ├─> Confirmación con advertencia                      │
│  ├─> Sistema genera certificados PDF                   │
│  ├─> Oferta cambia a estado CERRADA (inmutable)        │
│  └─> Envío de emails con certificados                  │
└─────────────────────────────────────────────────────────┘
```

## 🏗️ Arquitectura Implementada

### 1. Nuevos Enums

#### EstadoOferta (modificado)
```java
public enum EstadoOferta {
    ACTIVA,        // Inscripciones abiertas
    DE_BAJA,       // Cancelada
    ENCURSO,       // En desarrollo
    FINALIZADA,    // Terminó, pero notas NO cerradas
    CERRADA        // Notas cerradas, certificados emitidos - INMUTABLE
}
```

#### EstadoCertificacion (nuevo)
```java
public enum EstadoCertificacion {
    PENDIENTE,           // Alumno inscrito, oferta en curso
    PROPUESTA,           // Sistema propone (cumple criterios)
    APROBADO_DOCENTE,    // Docente aprobó manualmente
    RECHAZADO_DOCENTE,   // Docente rechazó
    CERTIFICADO_EMITIDO, // Certificado generado
    NO_APLICA            // No cumple criterios mínimos
}
```

### 2. Nueva Entidad: Certificacion

**Campos Principales:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `inscripcion` | Inscripciones | Referencia a la inscripción |
| `estado` | EstadoCertificacion | Estado actual del proceso |
| `promedioGeneral` | Double | Promedio de todas las actividades |
| `porcentajeAsistencia` | Double | % de clases asistidas |
| `tareasEntregadas` | Integer | Cantidad de tareas entregadas |
| `tareasTotales` | Integer | Total de tareas disponibles |
| `examenesAprobados` | Integer | Exámenes con nota ≥ 7 |
| `examenesTotales` | Integer | Total de exámenes |
| `cumpleCriteriosAutomaticos` | Boolean | true si pasa todos los filtros |
| `aprobadoDocente` | Boolean | Decisión del docente |
| `observacionesDocente` | String | Justificación docente |
| `numeroCertificado` | String | Ej: "AUREA-2026-CURSO-001234" |
| `certificadoEmitido` | Boolean | Si se generó el PDF |
| `urlCertificadoPdf` | String | Ruta al archivo |

**Métodos Importantes:**
```java
boolean verificarCriteriosMinimos(...)  // Valida si cumple requisitos
void aprobarPorDocente(...)             // Aprueba manualmente
void rechazarPorDocente(...)            // Rechaza con observaciones
void generarNumeroCertificado(...)      // Crea código único
```

### 3. Repository: CertificacionRepository

**Queries Principales:**
```java
Optional<Certificacion> findByInscripcion(Inscripciones inscripcion)
List<Certificacion> findByOferta(OfertaAcademica oferta)
List<Certificacion> findByOfertaAndEstado(OfertaAcademica oferta, EstadoCertificacion estado)
Long countPropuestasEnOferta(OfertaAcademica oferta)
Long countCertificadosEmitidosEnOferta(OfertaAcademica oferta)
List<Certificacion> findPendientesRevisionEnOferta(OfertaAcademica oferta)
Optional<Certificacion> findByNumeroCertificado(String numero)
```

### 4. Service: CertificacionService

**Métodos Públicos:**

| Método | Descripción | Cuándo se usa |
|--------|-------------|---------------|
| `calcularCertificacionesAutomaticas(oferta)` | Calcula quiénes califican | Cuando oferta → FINALIZADA |
| `aprobarManualmente(inscripcionId, docente, obs)` | Aprobar alumno extra | Docente agrega manualmente |
| `rechazarManualmente(certId, docente, obs)` | Quitar de la lista | Docente rechaza propuesta |
| `cerrarNotasYEmitirCertificados(ofertaId, docente)` | CIERRE FINAL | Botón "Cerrar Notas" |
| `obtenerResumenCertificaciones(ofertaId)` | Stats de la oferta | Vista certificaciones |

**Criterios Configurables (constantes en el service):**
```java
PROMEDIO_MINIMO = 7.0
ASISTENCIA_MINIMA = 75.0%
PORCENTAJE_TAREAS_MINIMO = 80.0%
PORCENTAJE_EXAMENES_MINIMO = 100.0%
```

**💡 Nota:** En futuras versiones, estos criterios pueden venir de la tabla `OfertaAcademica` o `Instituto` para ser configurables por admin.

## 🎨 Interfaz de Usuario Propuesta

### Ubicación en el Menú del Aula

Agregar nueva pestaña al mismo nivel que "Calificaciones":

```html
<!-- En aula.html o layout del docente -->
<ul class="nav-tabs">
    <li><a href="/aula/oferta/{{ofertaId}}/general">General</a></li>
    <li><a href="/aula/oferta/{{ofertaId}}/participantes">Participantes</a></li>
    <li><a href="/aula/oferta/{{ofertaId}}/asistencia">Asistencia</a></li>
    <li><a href="/aula/oferta/{{ofertaId}}/calificaciones">Calificaciones</a></li>
    <li><a href="/aula/oferta/{{ofertaId}}/certificaciones" class="new">
        🎓 Certificaciones
        <span class="badge" th:if="${propuestasPendientes > 0}">
            ${propuestasPendientes}
        </span>
    </a></li>
</ul>
```

### Vista: certificaciones.html

**Estructura propuesta:**

```
┌────────────────────────────────────────────────────────────┐
│  🎓 Gestión de Certificaciones - Introducción a Python    │
│                                                             │
│  Estado de la Oferta: FINALIZADA                           │
│  ⚠️ Las notas aún NO están cerradas                        │
├────────────────────────────────────────────────────────────┤
│  📊 RESUMEN                                                │
│  ┌──────────────────────────────────────────────────┐     │
│  │ Total Inscritos:        25                        │     │
│  │ Propuestos automático:  18  (✅ cumplen criterios)│     │
│  │ Aprobados por docente:  2   (➕ agregados manual) │     │
│  │ Rechazados:             3   (❌ quitados)         │     │
│  │ No cumplen criterios:   2   (⚠️ bajo rendimiento) │     │
│  │                                                    │     │
│  │ TOTAL A CERTIFICAR:     20                        │     │
│  └──────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────┤
│  📋 LISTADO DE ALUMNOS                                     │
│                                                             │
│  Filtros: [Todos] [Propuestos ✅] [Rechazados ❌]          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ Alumno              │ Prom │ Asist │ Estado │ Acción  │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ García, Juan        │ 8.5  │ 92%   │ ✅ Propuesto    │ │
│  │ [Detalle v]         │      │       │ [❌ Rechazar]   │ │
│  │                                                        │ │
│  │ López, María        │ 9.0  │ 88%   │ ✅ Propuesto    │ │
│  │                     │      │       │ [❌ Rechazar]   │ │
│  │                                                        │ │
│  │ Pérez, Carlos       │ 6.5  │ 70%   │ ⚠️ No cumple    │ │
│  │                     │      │       │ [➕ Aprobar]    │ │
│  │                                                        │ │
│  │ Díaz, Ana           │ 7.2  │ 95%   │ ❌ Rechazado    │ │
│  │ Obs: "Copió en ex." │      │       │ [✅ Reaprobar]  │ │
│  │                                                        │ │
│  │ Torres, Luis        │ 8.0  │ 85%   │ ➕ Manual       │ │
│  │ Obs: "Caso especial"│      │       │ [❌ Quitar]     │ │
│  └──────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────┤
│  ⚠️ ZONA DE PELIGRO                                        │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ Una vez cerradas las notas, NO SE PUEDEN MODIFICAR.  │ │
│  │                                                        │ │
│  │ [🔒 Cerrar Notas y Emitir Certificados]              │ │
│  │      (Requiere confirmación)                          │ │
│  └──────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**Modal de Confirmación (Cerrar Notas):**

```
┌─────────────────────────────────────────────────┐
│  ⚠️ CONFIRMAR CIERRE DE NOTAS                  │
├─────────────────────────────────────────────────┤
│  Esta acción es IRREVERSIBLE.                   │
│                                                  │
│  Se emitirán 20 certificados a:                 │
│  • 18 alumnos propuestos automáticamente        │
│  • 2 alumnos aprobados manualmente              │
│                                                  │
│  Una vez cerrada, la oferta cambiará a estado   │
│  CERRADA y NO podrás modificar calificaciones.  │
│                                                  │
│  ¿Estás seguro de continuar?                    │
│                                                  │
│  [Cancelar]  [✅ Sí, Cerrar Notas]              │
└─────────────────────────────────────────────────┘
```

## 🔌 Controller Necesario

Necesitas crear un nuevo controller:

```java
@Controller
@RequestMapping("/aula/oferta/{ofertaId}/certificaciones")
public class CertificacionController {
    
    @Autowired
    private CertificacionService certificacionService;
    
    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    // GET: Vista principal
    @GetMapping
    public String verCertificaciones(@PathVariable Long ofertaId, Model model) {
        // Cargar oferta, certificaciones, resumen
        // Renderizar certificaciones.html
    }
    
    // POST: Aprobar alumno manualmente
    @PostMapping("/aprobar/{inscripcionId}")
    public String aprobarManual(@PathVariable Long ofertaId, 
                                @PathVariable Long inscripcionId,
                                @RequestParam String observaciones,
                                Authentication auth) {
        // Obtener docente actual
        // certificacionService.aprobarManualmente(...)
        // Redirect a certificaciones
    }
    
    // POST: Rechazar alumno
    @PostMapping("/rechazar/{certificacionId}")
    public String rechazarAlumno(@PathVariable Long ofertaId,
                                 @PathVariable Long certificacionId,
                                 @RequestParam String observaciones,
                                 Authentication auth) {
        // certificacionService.rechazarManualmente(...)
    }
    
    // POST: CERRAR NOTAS (acción crítica)
    @PostMapping("/cerrar")
    public String cerrarNotas(@PathVariable Long ofertaId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        // Validar que oferta esté FINALIZADA
        // certificacionService.cerrarNotasYEmitirCertificados(...)
        // Mostrar resultado (success/error)
        // Redirect con mensaje
    }
}
```

## 🔄 Integración con Sistema Existente

### Trigger Automático al Finalizar Oferta

Opción 1: **Listener de cambio de estado**

```java
@Component
public class OfertaEstadoListener {
    
    @Autowired
    private CertificacionService certificacionService;
    
    @EventListener
    public void onOfertaFinalizada(OfertaFinalizadaEvent event) {
        System.out.println("🎓 Oferta finalizada, calculando certificaciones...");
        certificacionService.calcularCertificacionesAutomaticas(event.getOferta());
    }
}
```

Opción 2: **Llamada directa en AdminController**

```java
// En AdminController o DocenteController
@PostMapping("/admin/ofertas/{id}/finalizar")
public String finalizarOferta(@PathVariable Long id) {
    OfertaAcademica oferta = ofertaRepository.findById(id).orElseThrow();
    
    oferta.setEstado(EstadoOferta.FINALIZADA);
    ofertaRepository.save(oferta);
    
    // ✅ TRIGGER AUTOMÁTICO
    certificacionService.calcularCertificacionesAutomaticas(oferta);
    
    return "redirect:/admin/ofertas/" + id;
}
```

### Mostrar Badge de Notificación

```html
<!-- En el menú lateral del docente -->
<li>
    <a href="/aula/oferta/{{ofertaId}}/certificaciones">
        🎓 Certificaciones
        <span class="badge badge-warning" 
              th:if="${propuestasPendientes > 0}"
              th:text="${propuestasPendientes}">
            18
        </span>
    </a>
</li>
```

## 📧 Notificaciones por Email (Siguiente Fase)

Después de cerrar notas, enviar emails:

```java
// En CertificacionService.cerrarNotasYEmitirCertificados()

for (Certificacion cert : aprobados) {
    // ... generar certificado ...
    
    // Enviar email
    emailService.enviarCertificado(
        cert.getInscripcion().getAlumno().getEmail(),
        cert.getNumeroCertificado(),
        cert.getUrlCertificadoPdf()
    );
}
```

**Template de Email:**
```
Asunto: 🎓 Certificado de Finalización - [Nombre del Curso]

Estimado/a [Nombre],

¡Felicitaciones! Has completado exitosamente el curso/formación:
"[Nombre de la Oferta]"

Tu certificado está disponible para descarga:
[Botón: Descargar Certificado]

Número de certificado: AUREA-2026-CURSO-001234

---
Instituto Aurea
```

## 📊 Reportes y Estadísticas

Agregar al dashboard de admin:

```java
// En ReporteService
public Map<String, Object> obtenerEstadisticasCertificaciones() {
    return Map.of(
        "certificadosEmitidosHoy", certificacionRepo.countEmitidosHoy(),
        "promedioAprobacion", calcularPromedioAprobacion(),
        "topCursosConMasCertificados", obtenerTop10Cursos()
    );
}
```

## 🔐 Seguridad y Validaciones

### Permisos Requeridos

| Acción | Rol Requerido | Validación Adicional |
|--------|---------------|---------------------|
| Ver certificaciones | DOCENTE, ADMIN | Ser docente del curso |
| Aprobar/Rechazar | DOCENTE, ADMIN | Ser docente del curso |
| Cerrar notas | DOCENTE, ADMIN | Oferta en FINALIZADA |
| Re-abrir notas | ADMIN solamente | Solo si NO hay certificados emitidos |

### Validaciones de Negocio

```java
// Antes de cerrar notas
if (oferta.getEstado() != EstadoOferta.FINALIZADA) {
    throw new RuntimeException("Solo se pueden cerrar notas de ofertas FINALIZADAS");
}

if (certificacionRepository.countPropuestasEnOferta(oferta) == 0) {
    throw new RuntimeException("No hay alumnos aprobados para certificar");
}

// Verificar que todas las calificaciones estén cargadas
if (tieneCalificacionesPendientes(oferta)) {
    throw new RuntimeException("Aún hay calificaciones pendientes de cargar");
}
```

## 📁 Estructura de Archivos Creados

```
demo/src/main/java/com/example/demo/
├── enums/
│   ├── EstadoOferta.java           ✅ MODIFICADO (agregado CERRADA)
│   └── EstadoCertificacion.java    ✅ NUEVO
├── model/
│   └── Certificacion.java          ✅ NUEVO
├── repository/
│   └── CertificacionRepository.java ✅ NUEVO
├── service/
│   └── CertificacionService.java   ✅ NUEVO
└── controller/
    └── CertificacionController.java ⏳ PENDIENTE (tú debes crear)

demo/src/main/resources/templates/
└── aula/
    └── certificaciones.html         ⏳ PENDIENTE (vista HTML)
```

## ✅ Checklist de Implementación

### Fase 1: Backend (Completado ✅)
- [x] Enum `EstadoCertificacion`
- [x] Modificar `EstadoOferta` (agregar CERRADA)
- [x] Entidad `Certificacion`
- [x] Repository `CertificacionRepository`
- [x] Service `CertificacionService`

### Fase 2: Controller y Rutas (Pendiente ⏳)
- [ ] Crear `CertificacionController`
- [ ] Endpoint GET `/aula/oferta/{id}/certificaciones`
- [ ] Endpoint POST `/aula/oferta/{id}/certificaciones/aprobar/{inscripcionId}`
- [ ] Endpoint POST `/aula/oferta/{id}/certificaciones/rechazar/{certId}`
- [ ] Endpoint POST `/aula/oferta/{id}/certificaciones/cerrar`

### Fase 3: Frontend (Pendiente ⏳)
- [ ] Crear `certificaciones.html`
- [ ] Tabla de alumnos con filtros
- [ ] Botones de acción (aprobar/rechazar)
- [ ] Modal de confirmación de cierre
- [ ] Badge de notificación en menú
- [ ] Estilos CSS

### Fase 4: Integración (Pendiente ⏳)
- [ ] Trigger automático al finalizar oferta
- [ ] Agregar pestaña en menú del aula
- [ ] Mostrar contador de pendientes
- [ ] Bloquear edición si oferta CERRADA

### Fase 5: Generación de PDFs (Futuro)
- [ ] Template de certificado
- [ ] Generación con iText o similar
- [ ] Firma digital opcional
- [ ] Código QR de verificación

### Fase 6: Emails (Futuro)
- [ ] Template de email
- [ ] Adjuntar PDF certificado
- [ ] Envío masivo al cerrar notas

## 🚀 Próximos Pasos Inmediatos

1. **Compilar y verificar:**
   ```bash
   mvnw clean install
   ```

2. **Crear el Controller** (te lo haré en el siguiente paso)

3. **Crear la vista HTML** `certificaciones.html`

4. **Probar el flujo:**
   - Finalizar una oferta
   - Ver que se calculen certificaciones
   - Aprobar/rechazar alumnos
   - Cerrar notas
   - Verificar estado CERRADA

5. **Ajustar criterios** según necesidades institucionales

## 💡 Consideraciones Importantes

### ¿Qué pasa con ofertas antiguas ya finalizadas?

Ejecutar migración una sola vez:

```java
@Component
public class MigracionCertificaciones {
    
    @Autowired
    private CertificacionService certificacionService;
    
    @Autowired
    private OfertaAcademicaRepository ofertaRepo;
    
    @PostConstruct
    public void migrarOfertasFinalizadas() {
        List<OfertaAcademica> finalizadas = ofertaRepo.findByEstado(EstadoOferta.FINALIZADA);
        
        for (OfertaAcademica oferta : finalizadas) {
            try {
                certificacionService.calcularCertificacionesAutomaticas(oferta);
                System.out.println("✅ Migrada: " + oferta.getNombre());
            } catch (Exception e) {
                System.err.println("❌ Error en: " + oferta.getNombre());
            }
        }
    }
}
```

### ¿Se pueden modificar notas después de cerrar?

**NO**. Una vez en estado `CERRADA`, la oferta es **inmutable**. 

Si necesitas hacer correcciones:
1. Solo ADMIN puede "reabrir" (si NO hay certificados emitidos)
2. Si ya hay certificados emitidos, se requiere proceso manual:
   - Anular certificado anterior
   - Emitir certificado nuevo con nota corregida

---

**Siguiente paso:** ¿Quieres que cree el `CertificacionController` completo con todos los endpoints?
