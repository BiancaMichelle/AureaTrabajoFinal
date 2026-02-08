package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoExamen;
import com.example.demo.event.ActivityCreatedEvent;
import com.example.demo.model.Curso;
import com.example.demo.model.Examen;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ExamenRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;

@Service
public class ExamenService {

    private static final Logger log = LoggerFactory.getLogger(ExamenService.class);

    @Autowired
    private ExamenRepository examenRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PoolRepository poolRepository;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private AnalisisRendimientoService analisisRendimientoService;

    @Autowired
    private IaPoolGeneratorService iaPoolGeneratorService;

    @Transactional
    public Examen crearExamen(Examen examen, UUID moduloId, List<PoolDTO> poolsDTO, List<UUID> modulosRelacionadosIds) {
        // Buscar el módulo
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

        // Validar fechas
        if (examen.getFechaApertura() != null && examen.getFechaCierre() != null &&
                examen.getFechaCierre().isBefore(examen.getFechaApertura())) {
            throw new RuntimeException("La fecha de cierre no puede ser anterior a la fecha de apertura");
        }

        // Configurar el examen
        examen.setModulo(modulo);
        examen.setFechaCreacion(LocalDateTime.now());
        examen.setEstado(EstadoExamen.PENDIENTE);

        // Guardar el examen primero
        Examen examenGuardado = examenRepository.save(examen);

        // Asociar módulos relacionados para contexto IA
        List<Modulo> modulosRelacionados = new ArrayList<>();
        if (modulosRelacionadosIds != null && !modulosRelacionadosIds.isEmpty()) {
            modulosRelacionados = moduloRepository.findAllById(modulosRelacionadosIds);
        }
        if (!modulosRelacionados.contains(modulo)) {
            modulosRelacionados.add(modulo);
        }
        examenGuardado.setModulosRelacionados(modulosRelacionados);
        examenRepository.save(examenGuardado);

        // Procesar los pools
        if (poolsDTO != null && !poolsDTO.isEmpty()) {
            List<Pool> pools = new ArrayList<>();
            java.util.Set<String> poolsUsados = new java.util.HashSet<>();

            for (PoolDTO poolDTO : poolsDTO) {
                Pool poolGuardado;

                // Validar duplicados si es reutilización
                if (poolDTO.getIdReal() != null && !poolDTO.getIdReal().isEmpty()) {
                    if (!poolsUsados.add(poolDTO.getIdReal())) {
                        throw new RuntimeException("No se puede agregar el mismo pool más de una vez.");
                    }

                    poolGuardado = poolRepository.findById(UUID.fromString(poolDTO.getIdReal()))
                            .orElseThrow(
                                    () -> new RuntimeException("Pool existente no encontrado: " + poolDTO.getIdReal()));

                    // Validar que el pool existente tenga preguntas
                    if (poolGuardado.getPreguntas() == null || poolGuardado.getPreguntas().isEmpty()) {
                        throw new RuntimeException("El pool '" + poolGuardado.getNombre()
                                + "' no tiene preguntas. Agrega preguntas antes de crear el examen.");
                    }

                    // Validar estado IA
                    if (poolGuardado.getIaStatus() == com.example.demo.enums.IaGenerationStatus.PENDING) {
                        throw new RuntimeException("El pool '" + poolGuardado.getNombre()
                                + "' aún se está generando con IA. Espera a que termine.");
                    }

                } else {
                    // Validaciones para pool nuevo
                    if (poolDTO.getPreguntas() == null || poolDTO.getPreguntas().isEmpty()) {
                        throw new RuntimeException("El nuevo pool '" + poolDTO.getNombre() + "' no tiene preguntas.");
                    }

                    // Crear un nuevo pool y asociarlo a la oferta del módulo
                    Pool pool = new Pool();
                    pool.setIdPool(UUID.randomUUID());
                    pool.setNombre(poolDTO.getNombre());
                    pool.setDescripcion(poolDTO.getDescripcion());
                    pool.setCantidadPreguntas(poolDTO.getCantidadPreguntas());
                    pool.setOferta(modulo.getCurso());

                    // Guardar el pool
                    poolGuardado = poolRepository.save(pool);

                    // Procesar las preguntas del pool (si vinieron inline)
                    if (poolDTO.getPreguntas() != null && !poolDTO.getPreguntas().isEmpty()) {
                        List<Pregunta> preguntas = new ArrayList<>();
                        for (PreguntaDTO preguntaDTO : poolDTO.getPreguntas()) {
                            Pregunta pregunta = new Pregunta();
                            pregunta.setIdPregunta(UUID.randomUUID());
                            pregunta.setEnunciado(preguntaDTO.getEnunciado());
                            pregunta.setTipoPregunta(preguntaDTO.getTipoPregunta());
                            pregunta.setPuntaje(preguntaDTO.getPuntaje());
                            pregunta.setPool(poolGuardado);
                            preguntas.add(preguntaRepository.save(pregunta));
                        }
                        poolGuardado.setPreguntas(preguntas);
                    }
                }
                pools.add(poolGuardado);
            }

            // Asignar los pools al examen (lado owner de la relación ManyToMany)
            examenGuardado.setPoolPreguntas(pools);
            examenRepository.save(examenGuardado);
        }

        eventPublisher.publishEvent(new ActivityCreatedEvent(
                modulo.getCurso().getIdOferta(),
                examenGuardado.getIdActividad(),
                "EXAMEN",
                examenGuardado.getFechaCierre(),
                examenGuardado.getTitulo()));

        // ✅ Analizar rendimiento y generar pre-examen si es necesario
        analizarYGenerarPreExamen(examenGuardado, modulo);

        return examenGuardado;
    }

    /**
     * Analiza el rendimiento de los alumnos y genera un pre-examen automático
     * si ≥50% tienen bajo rendimiento
     */
    @Async
    @Transactional
    public void analizarYGenerarPreExamen(Examen examenPrincipal, Modulo modulo) {
        try {
            log.info("🔍 Analizando rendimiento para posible generación de pre-examen");

            // 1. Obtener la oferta académica del módulo
            OfertaAcademica ofertaBase = modulo.getCurso();
            if (ofertaBase == null) {
                log.warn("No se encontró oferta académica para el módulo");
                return;
            }

            // DEBUG: Ver qué tipo de objeto tenemos
            log.info("🔍 DEBUG - Tipo de ofertaBase: {}", ofertaBase.getClass().getName());
            log.info("🔍 DEBUG - Es instanceof Curso: {}", ofertaBase instanceof Curso);
            log.info("🔍 DEBUG - Es instanceof OfertaAcademica: {}", ofertaBase instanceof OfertaAcademica);

            // CRÍTICO: Desempaquetar el proxy de Hibernate ANTES del cast
            // Hibernate usa proxies para lazy loading, entonces instanceof falla
            Curso curso = null;
            try {
                // Obtener la clase real
                Class<?> claseReal = Hibernate.getClass(ofertaBase);
                log.info("🔍 DEBUG - Clase real (Hibernate.getClass): {}", claseReal.getName());

                // Verificar si la clase real es Curso
                if (!Curso.class.isAssignableFrom(claseReal)) {
                    log.warn("La oferta no es un Curso (clase real: {}), no se puede analizar rendimiento",
                            claseReal.getSimpleName());
                    return;
                }

                // AQUÍ ESTÁ LA CLAVE: Desempaquetar el proxy ANTES del cast
                Object ofertaDesempaquetada = Hibernate.unproxy(ofertaBase);
                log.info("🔍 DEBUG - Tipo después de unproxy: {}", ofertaDesempaquetada.getClass().getName());
                log.info("🔍 DEBUG - És instanceof Curso después de unproxy: {}",
                        ofertaDesempaquetada instanceof Curso);

                // Ahora SÍ podemos hacer el cast seguro
                curso = (Curso) ofertaDesempaquetada;
                log.info("✅ Cast exitoso a Curso: {}", curso.getNombre());

            } catch (Exception e) {
                log.error("Error al desempaquetar proxy de Hibernate o al castear a Curso: {}", e.getMessage(), e);
                return;
            }

            // 2. Obtener inscripciones activas
            List<Inscripciones> inscripciones = inscripcionRepository
                    .findByOfertaAndEstadoInscripcionTrue(curso);

            if (inscripciones == null || inscripciones.isEmpty()) {
                log.info("No hay inscripciones activas, no se genera pre-examen");
                return;
            }

            // 3. Calcular métricas de rendimiento
            int alumnosConRendimientoBajo = 0;
            int totalAlumnos = inscripciones.size();

            for (Inscripciones insc : inscripciones) {
                Usuario alumno = insc.getAlumno();
                if (alumno == null)
                    continue;

                double promedio = analisisRendimientoService.calcularPromedio(alumno, curso);
                double asistencia = analisisRendimientoService.calcularAsistencia(alumno, curso);

                // DEBUG: Mostrar valores reales
                log.info("🔍 DEBUG - Alumno: {}, Promedio: {}, Asistencia: {}%",
                        alumno.getNombre(), promedio, asistencia);

                // Criterio: bajo rendimiento si promedio < 6 O asistencia < 70%
                // 100.0 es valor centinela para "sin notas"
                // Soportamos AMBAS escalas: 0-10 (< 6) y 0-100 (< 60)
                boolean tienePromediosBajos = promedio != 100.0 &&
                        (promedio < 6.0 || (promedio > 10.0 && promedio < 60.0));
                boolean tieneAsistenciaBaja = asistencia < 70.0;

                log.info("🔍 DEBUG - Condiciones: promedioBajo={}, asistenciaBaja={}, promedio!=100={}",
                        tienePromediosBajos, tieneAsistenciaBaja, promedio != 100.0);

                if (tienePromediosBajos || tieneAsistenciaBaja) {
                    alumnosConRendimientoBajo++;
                    log.info("⚠️ Alumno {} detectado con BAJO RENDIMIENTO - Promedio: {}, Asistencia: {}%",
                            alumno.getNombre(), promedio, asistencia);
                }
            }

            // 4. Decidir si generar pre-examen (≥50% con bajo rendimiento)
            double porcentajeBajoRendimiento = (double) alumnosConRendimientoBajo / totalAlumnos * 100;

            log.info("📊 Resultado análisis: {}/{} alumnos ({}%) con bajo rendimiento",
                    alumnosConRendimientoBajo, totalAlumnos,
                    String.format("%.1f", porcentajeBajoRendimiento));

            if (porcentajeBajoRendimiento >= 50.0) {
                log.info("🎯 ≥50% de alumnos con bajo rendimiento. Generando pre-examen...");
                generarPreExamenAutomatico(examenPrincipal, modulo);
            } else {
                log.info("✅ Solo {}% con bajo rendimiento. No se requiere pre-examen",
                        String.format("%.1f", porcentajeBajoRendimiento));
            }

        } catch (Exception e) {
            log.error("❌ Error al analizar rendimiento para pre-examen: {}", e.getMessage(), e);
        }
    }

    /**
     * Genera un pre-examen automático basado en el examen principal
     * El pre-examen queda oculto para revisión del docente
     */
    @Transactional
    private void generarPreExamenAutomatico(Examen examenPrincipal, Modulo modulo) {
        try {
            log.info("📝 Generando pre-examen para: {}", examenPrincipal.getTitulo());

            // 1. Crear nuevo examen (pre-examen)
            Examen preExamen = new Examen();
            preExamen.setTitulo("Propuesta PreExamen (" + examenPrincipal.getTitulo() + ")");
            preExamen.setDescripcion("Pre-examen de práctica generado automáticamente. " +
                    "Revisa y modifica antes de publicar a los alumnos.");

            // 2. Marcar como oculto y como pre-examen generado
            preExamen.setVisibilidad(false); // ⭐ OCULTO para el alumno
            preExamen.setGenerarPreExamen(true); // ⭐ Marca que es pre-examen auto-generado

            // 3. Copiar configuración del examen principal
            preExamen.setModulo(modulo);
            preExamen.setFechaCreacion(LocalDateTime.now());
            preExamen.setEstado(EstadoExamen.PENDIENTE);
            preExamen.setTiempoRealizacion(examenPrincipal.getTiempoRealizacion());
            preExamen.setCantidadIntentos(3); // Más intentos para práctica
            preExamen.setCalificacionAutomatica(true);
            preExamen.setPublicarNota(true);

            // 4. Establecer fechas antes del examen principal
            LocalDateTime aperturaOriginal = examenPrincipal.getFechaApertura();
            if (aperturaOriginal != null) {
                preExamen.setFechaApertura(aperturaOriginal.minusDays(3)); // 3 días antes
                preExamen.setFechaCierre(aperturaOriginal.minusHours(1)); // 1 hora antes
            } else {
                // Si no hay fecha de apertura, usar fechas relativas a hoy
                preExamen.setFechaApertura(LocalDateTime.now());
                preExamen.setFechaCierre(LocalDateTime.now().plusDays(7));
            }

            // 5. GENERAR 8 PREGUNTAS CON IA del material del módulo
            log.info("🤖 Generando 8 preguntas con IA del material del módulo...");

            // crear un nuevo pool para el pre-examen
            Pool poolPreExamen = new Pool();
            poolPreExamen.setNombre("Pool Auto-Generado: " + modulo.getNombre());
            poolPreExamen.setDescripcion("Generado automáticamente con IA para pre-examen de práctica");
            poolPreExamen.setCantidadPreguntas(8); // 8 preguntas
            poolPreExamen.setGeneratedByIA(true);
            poolPreExamen.setIaStatus(com.example.demo.enums.IaGenerationStatus.PENDING);

            // Preparar parámetros para la generación con IA
            String paramsJson = String.format(
                    "{\"tema\": \"%s\", \"cantidadPreguntas\": 8, \"contextoMaterial\": \"%s\", \"objetivos\": \"%s\", \"bibliografia\": \"%s\"}",
                    escaparJson(modulo.getNombre()),
                    escaparJson(modulo.getTemario() != null ? modulo.getTemario() : "Contenido del módulo"),
                    escaparJson(modulo.getObjetivos() != null ? modulo.getObjetivos() : ""),
                    escaparJson(modulo.getBibliografia() != null ? modulo.getBibliografia() : ""));

            poolPreExamen.setIaRequest(paramsJson);

            // Guardar el pool
            Pool poolGuardado = poolRepository.save(poolPreExamen);

            log.info("📄 Pool creado, lanzando generación IA asíncrona...");

            // Lanzar generación ASÍNCRONA (no bloqueante)
            iaPoolGeneratorService.generarPoolIAAsync(poolGuardado.getIdPool(), paramsJson);

            // Asociar el pool al pre-examen (aunque aún no tenga preguntas)
            // Las preguntas se generarán en background
            List<Pool> poolsPreExamen = new ArrayList<>();
            poolsPreExamen.add(poolGuardado);
            preExamen.setPoolPreguntas(poolsPreExamen);

            // Copiar módulos relacionados si existen
            List<Modulo> modulosRel = examenPrincipal.getModulosRelacionados();
            if (modulosRel != null && !modulosRel.isEmpty()) {
                preExamen.setModulosRelacionados(new ArrayList<>(modulosRel));
            }

            // 6. Guardar pre-examen
            Examen preExamenGuardado = examenRepository.save(preExamen);

            log.info("✅ Pre-examen generado automáticamente:");
            log.info("   - ID: {}", preExamenGuardado.getIdActividad());
            log.info("   - Título: {}", preExamenGuardado.getTitulo());
            log.info("   - Visible: {}", preExamenGuardado.getVisibilidad());
            log.info("   - Pool IA: Generándose en background con 8 preguntas");

            // NO publicar evento ActivityCreatedEvent para evitar notificaciones prematuras
            // El docente debe revisar y publicar manualmente

        } catch (Exception e) {
            log.error("❌ Error al generar pre-examen automático: {}", e.getMessage(), e);
        }
    }

    // DTOs internos para transferencia de datos
    public static class PoolDTO {
        private String idReal; // UUID en string
        private Boolean esExistente;
        private String nombre;
        private String descripcion;
        private Integer cantidadPreguntas;
        private List<PreguntaDTO> preguntas;

        // Getters y Setters
        public String getIdReal() {
            return idReal;
        }

        public void setIdReal(String idReal) {
            this.idReal = idReal;
        }

        public Boolean getEsExistente() {
            return esExistente;
        }

        public void setEsExistente(Boolean esExistente) {
            this.esExistente = esExistente;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public Integer getCantidadPreguntas() {
            return cantidadPreguntas;
        }

        public void setCantidadPreguntas(Integer cantidadPreguntas) {
            this.cantidadPreguntas = cantidadPreguntas;
        }

        public List<PreguntaDTO> getPreguntas() {
            return preguntas;
        }

        public void setPreguntas(List<PreguntaDTO> preguntas) {
            this.preguntas = preguntas;
        }
    }

    public static class PreguntaDTO {
        private String enunciado;
        private com.example.demo.enums.TipoPregunta tipoPregunta;
        private Float puntaje;

        // Getters y Setters
        public String getEnunciado() {
            return enunciado;
        }

        public void setEnunciado(String enunciado) {
            this.enunciado = enunciado;
        }

        public com.example.demo.enums.TipoPregunta getTipoPregunta() {
            return tipoPregunta;
        }

        public void setTipoPregunta(com.example.demo.enums.TipoPregunta tipoPregunta) {
            this.tipoPregunta = tipoPregunta;
        }

        public Float getPuntaje() {
            return puntaje;
        }

        public void setPuntaje(Float puntaje) {
            this.puntaje = puntaje;
        }
    }

    @Transactional
    public void eliminarActividad(Long actividadId) {
        Examen examen = examenRepository.findById(actividadId)
                .orElseThrow(() -> new RuntimeException("Actividad no encontrada"));
        examenRepository.delete(examen);
    }

    /**
     * Escapa caracteres especiales para JSON
     */
    private String escaparJson(String texto) {
        if (texto == null)
            return "";
        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
