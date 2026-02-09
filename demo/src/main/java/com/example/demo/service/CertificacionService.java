package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.enums.EstadoCertificacion;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.*;
import com.example.demo.repository.*;

/**
 * Servicio para gestionar el proceso de certificación de alumnos.
 * 
 * FLUJO:
 * 1. Cuando oferta pasa a FINALIZADA → calcularCertificacionesAutomaticas()
 * 2. Docente revisa propuestas en /aula/oferta/{id}/certificaciones
 * 3. Docente aprueba/rechaza/agrega alumnos
 * 4. Docente cierra notas → cerrarNotasYEmitirCertificados()
 * 5. Sistema genera PDFs y envía emails
 */
@Service
@Transactional
public class CertificacionService {
    
    @Autowired
    private CertificacionRepository certificacionRepository;
    
    @Autowired
    private InscripcionRepository inscripcionRepository;
    
    @Autowired
    private IntentoRepository intentoRepository;
    
    @Autowired
    private EntregaRepository entregaRepository;
    
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    
    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    
    // ========== CONFIGURACIÓN DE CRITERIOS (pueden venir de BD en el futuro) ==========
    
    private static final Double PROMEDIO_MINIMO = 7.0;
    private static final Double ASISTENCIA_MINIMA = 75.0;  // 75%
    private static final Double PORCENTAJE_TAREAS_MINIMO = 80.0;  // 80% de tareas entregadas
    private static final Double PORCENTAJE_EXAMENES_MINIMO = 100.0;  // 100% de exámenes aprobados
    
    // ========== CÁLCULO AUTOMÁTICO DE CERTIFICACIONES ==========
    
    /**
     * Calcula automáticamente qué alumnos califican para certificación.
     * Este método se ejecuta cuando la oferta pasa a estado FINALIZADA.
     * 
     * @param oferta La oferta académica que finalizó
     * @return Lista de certificaciones creadas/actualizadas
     */
    public List<Certificacion> calcularCertificacionesAutomaticas(OfertaAcademica oferta) {
        return calcularCertificacionesAutomaticas(oferta, null);
    }

    public List<Certificacion> calcularCertificacionesAutomaticas(OfertaAcademica ofertaParam, Double promedioMinimoOverride) {
        // Recargar la entidad para asegurar que esté adjunta a la sesión actual (evita LazyInitializationException en llamadas asíncronas)
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaParam.getIdOferta())
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada: " + ofertaParam.getIdOferta()));
                
        System.out.println("🎓 Calculando certificaciones automáticas para: " + oferta.getNombre());
        double promedioMinimo = promedioMinimoOverride != null ? promedioMinimoOverride : PROMEDIO_MINIMO;
        
        List<Inscripciones> inscripciones = inscripcionRepository.findByOfertaAndEstadoInscripcionTrue(oferta);
        List<Certificacion> certificaciones = new ArrayList<>();
        
        for (Inscripciones inscripcion : inscripciones) {
            Certificacion cert = certificacionRepository.findByInscripcion(inscripcion)
                .orElse(new Certificacion());
            
            if (cert.getIdCertificacion() == null) {
                cert.setInscripcion(inscripcion);
            }
            
            // Calcular métricas
            cert.setPromedioGeneral(calcularPromedioGeneral(inscripcion, oferta));
            cert.setPorcentajeAsistencia(calcularPorcentajeAsistencia(inscripcion, oferta));
            cert.setTareasEntregadas(contarTareasEntregadas(inscripcion, oferta));
            cert.setTareasTotales(contarTareasTotales(oferta));
            cert.setExamenesAprobados(contarExamenesAprobados(inscripcion, oferta, promedioMinimo));
            cert.setExamenesTotales(contarExamenesTotales(oferta));
            
            // Verificar si cumple criterios
            boolean cumple = cert.verificarCriteriosMinimos(
                promedioMinimo,
                ASISTENCIA_MINIMA,
                PORCENTAJE_TAREAS_MINIMO,
                PORCENTAJE_EXAMENES_MINIMO
            );
            
            cert.setCumpleCriteriosAutomaticos(cumple);
            cert.setFechaCalculoAutomatico(LocalDateTime.now());
            
            // Establecer estado según resultado
            if (cumple) {
                cert.setEstado(EstadoCertificacion.PROPUESTA);
                System.out.println("✅ " + inscripcion.getAlumno().getNombre() + " " + 
                    inscripcion.getAlumno().getApellido() + " - PROPUESTO para certificación");
            } else {
                cert.setEstado(EstadoCertificacion.NO_APLICA);
                System.out.println("❌ " + inscripcion.getAlumno().getNombre() + " " + 
                    inscripcion.getAlumno().getApellido() + " - NO cumple criterios");
            }
            
            certificaciones.add(certificacionRepository.save(cert));
        }
        
        System.out.println("📊 Resultado: " + 
            certificaciones.stream().filter(c -> c.getEstado() == EstadoCertificacion.PROPUESTA).count() + 
            " alumnos propuestos de " + certificaciones.size() + " totales");
        
        return certificaciones;
    }
    
    // ========== CÁLCULO DE MÉTRICAS ==========
    
    private Double calcularPromedioGeneral(Inscripciones inscripcion, OfertaAcademica oferta) {
        List<Double> calificaciones = new ArrayList<>();
        
        // Obtener calificaciones de exámenes
        List<Intento> intentos = intentoRepository.findByAlumno(inscripcion.getAlumno());
        for (Intento intento : intentos) {
            if (intento.getCalificacion() != null) {
                calificaciones.add(intento.getCalificacion().doubleValue());
            }
        }
        
        // Obtener calificaciones de tareas
        List<Entrega> entregas = entregaRepository.findByEstudiante(inscripcion.getAlumno());
        for (Entrega entrega : entregas) {
            if (entrega.getCalificacion() != null) {
                calificaciones.add(entrega.getCalificacion());
            }
        }
        
        if (calificaciones.isEmpty()) {
            return 0.0;
        }
        
        return calificaciones.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    private Double calcularPorcentajeAsistencia(Inscripciones inscripcion, OfertaAcademica oferta) {
        List<Asistencia> asistencias = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(
            oferta.getIdOferta(), 
            inscripcion.getAlumno().getDni()
        );
        
        if (asistencias.isEmpty()) {
            return 100.0;  // Si no hay registro, asumimos 100%
        }
        
        long presente = asistencias.stream()
            .filter(a -> a.getEstado() == EstadoAsistencia.PRESENTE)
            .count();
        
        return (double) presente / asistencias.size() * 100;
    }
    
    private Integer contarTareasEntregadas(Inscripciones inscripcion, OfertaAcademica oferta) {
        return entregaRepository.findByEstudiante(inscripcion.getAlumno()).size();
    }
    
    private Integer contarTareasTotales(OfertaAcademica oferta) {
        int total = 0;
        
        if (oferta instanceof Curso) {
            Curso curso = (Curso) oferta;
            for (Modulo modulo : curso.getModulos()) {
                for (Actividad actividad : modulo.getActividades()) {
                    if (actividad instanceof Tarea) {
                        total++;
                    }
                }
            }
        }
        
        return total;
    }
    
    private Integer contarExamenesAprobados(Inscripciones inscripcion, OfertaAcademica oferta, double promedioMinimo) {
        List<Intento> intentos = intentoRepository.findByAlumno(inscripcion.getAlumno());
        return (int) intentos.stream()
            .filter(i -> i.getCalificacion() != null && i.getCalificacion() >= promedioMinimo)
            .map(i -> i.getExamen().getIdActividad())
            .distinct()
            .count();
    }
    
    private Integer contarExamenesTotales(OfertaAcademica oferta) {
        int total = 0;
        
        if (oferta instanceof Curso) {
            Curso curso = (Curso) oferta;
            for (Modulo modulo : curso.getModulos()) {
                for (Actividad actividad : modulo.getActividades()) {
                    if (actividad instanceof Examen) {
                        total++;
                    }
                }
            }
        }
        
        return total;
    }
    
    // ========== GESTIÓN MANUAL POR DOCENTE ==========
    
    /**
     * Aprobar manualmente a un alumno para certificación
     */
    public Certificacion aprobarManualmente(Long inscripcionId, Docente docente, String observaciones) {
        Inscripciones inscripcion = inscripcionRepository.findById(inscripcionId)
            .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));
        
        Certificacion cert = certificacionRepository.findByInscripcion(inscripcion)
            .orElse(new Certificacion());
        
        if (cert.getIdCertificacion() == null) {
            cert.setInscripcion(inscripcion);
            // Calcular métricas si no existen
            if (cert.getPromedioGeneral() == null) {
                cert.setPromedioGeneral(calcularPromedioGeneral(inscripcion, inscripcion.getOferta()));
                cert.setPorcentajeAsistencia(calcularPorcentajeAsistencia(inscripcion, inscripcion.getOferta()));
            }
        }
        
        cert.aprobarPorDocente(docente, observaciones);
        
        System.out.println("✅ Docente " + docente.getNombre() + " aprobó manualmente a " + 
            inscripcion.getAlumno().getNombre());
        
        return certificacionRepository.save(cert);
    }
    
    /**
     * Rechazar a un alumno para certificación
     */
    public Certificacion rechazarManualmente(Long certificacionId, Docente docente, String observaciones) {
        Certificacion cert = certificacionRepository.findById(certificacionId)
            .orElseThrow(() -> new RuntimeException("Certificación no encontrada"));
        
        cert.rechazarPorDocente(docente, observaciones);
        
        System.out.println("❌ Docente " + docente.getNombre() + " rechazó a " + 
            cert.getInscripcion().getAlumno().getNombre());
        
        return certificacionRepository.save(cert);
    }
    
    // ========== CIERRE FINAL Y EMISIÓN ==========
    
    /**
     * Cierra las notas de la oferta y registra la lista de aprobados.
     * Este es el paso FINAL e IRREVERSIBLE.
     * Genera un ACTA DE CIERRE con la lista completa de aprobados.
     */
    public CierreNotasResult cerrarNotasYEmitirCertificados(Long ofertaId, Docente docente) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
            .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        if (oferta.getEstado() == EstadoOferta.CERRADA) {
            throw new RuntimeException("Las notas de esta oferta ya fueron cerradas. No se pueden modificar.");
        }
        
        if (oferta.getEstado() != EstadoOferta.FINALIZADA) {
            throw new RuntimeException("Solo se pueden cerrar notas de ofertas en estado FINALIZADA");
        }
        
        // Obtener todos los aprobados (PROPUESTA o APROBADO_DOCENTE)
        List<Certificacion> aprobados = certificacionRepository.findByOferta(oferta).stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.PROPUESTA || 
                         c.getEstado() == EstadoCertificacion.APROBADO_DOCENTE)
            .toList();
        
        if (aprobados.isEmpty()) {
            throw new RuntimeException("No hay alumnos aprobados para cerrar las notas");
        }
        
        System.out.println("🔒 Cerrando notas de: " + oferta.getNombre());
        System.out.println("📜 Registrando " + aprobados.size() + " alumnos aprobados...");
        
        int exitosos = 0;
        int fallidos = 0;
        List<String> errores = new ArrayList<>();
        
        // Registrar cada aprobado
        for (Certificacion cert : aprobados) {
            try {
                // Guardar para obtener ID si es nuevo
                cert = certificacionRepository.save(cert);
                
                // Generar número de registro único
                cert.generarNumeroCertificado(oferta);
                
                // Marcar como certificado emitido (en este caso = incluido en acta)
                cert.setCertificadoEmitido(true);
                cert.setFechaEmisionCertificado(LocalDateTime.now());
                cert.setEstado(EstadoCertificacion.CERTIFICADO_EMITIDO);
                
                certificacionRepository.save(cert);
                
                System.out.println("✅ Registrado en acta: " + cert.getNumeroCertificado() + 
                    " - " + cert.getInscripcion().getAlumno().getNombre() + " " +
                    cert.getInscripcion().getAlumno().getApellido() +
                    " (Promedio: " + cert.getPromedioGeneral() + ")");
                
                exitosos++;
                
            } catch (Exception e) {
                fallidos++;
                String error = "Error con " + cert.getInscripcion().getAlumno().getNombre() + 
                    ": " + e.getMessage();
                errores.add(error);
                System.err.println("❌ " + error);
            }
        }
        
        // CAMBIAR ESTADO DE LA OFERTA A CERRADA (INMUTABLE)
        oferta.setEstado(EstadoOferta.CERRADA);
        oferta.setEstadoProcesoCertificacion(com.example.demo.enums.EstadoProcesoCertificacion.EN_GESTION_CERTIFICACION);
        ofertaAcademicaRepository.save(oferta);
        
        System.out.println("✅ OFERTA CERRADA. " + exitosos + " alumnos aprobados registrados, " + 
            fallidos + " errores");
        System.out.println("📄 Acta de cierre enviada a administración");
        
        return new CierreNotasResult(exitosos, fallidos, errores, oferta);
    }
    
    /**
     * Obtiene el resumen de certificaciones de una oferta
     */
    public ResumenCertificaciones obtenerResumenCertificaciones(Long ofertaId) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
            .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        List<Certificacion> todas = certificacionRepository.findByOferta(oferta);
        
        long propuestas = todas.stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.PROPUESTA)
            .count();
        
        long aprobadosDocente = todas.stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.APROBADO_DOCENTE)
            .count();
        
        long rechazados = todas.stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.RECHAZADO_DOCENTE)
            .count();
        
        long emitidos = todas.stream()
            .filter(c -> Boolean.TRUE.equals(c.getCertificadoEmitido()))
            .count();
        
        long noAplican = todas.stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.NO_APLICA)
            .count();
        
        return new ResumenCertificaciones(
            todas.size(),
            propuestas,
            aprobadosDocente,
            rechazados,
            emitidos,
            noAplican,
            oferta.getEstado() == EstadoOferta.CERRADA
        );
    }
    
    /**
     * Obtiene todas las certificaciones de una oferta
     */
    public List<Certificacion> obtenerCertificacionesPorOferta(Long ofertaId) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
            .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        return certificacionRepository.findByOferta(oferta);
    }
    
    // ========== CLASES AUXILIARES ==========
    
    public static class CierreNotasResult {
        public int certificadosEmitidos;
        public int errores;
        public List<String> detallesErrores;
        public OfertaAcademica oferta;
        
        public CierreNotasResult(int emitidos, int errores, List<String> detalles, OfertaAcademica oferta) {
            this.certificadosEmitidos = emitidos;
            this.errores = errores;
            this.detallesErrores = detalles;
            this.oferta = oferta;
        }
    }
    
    public static class ResumenCertificaciones {
        public long totalInscritos;
        public long propuestas;
        public long aprobadosDocente;
        public long rechazados;
        public long certificadosEmitidos;
        public long noCumplenCriterios;
        public boolean ofertaCerrada;
        
        public ResumenCertificaciones(long total, long prop, long aprobados, long rech, 
                                     long emitidos, long noAplican, boolean cerrada) {
            this.totalInscritos = total;
            this.propuestas = prop;
            this.aprobadosDocente = aprobados;
            this.rechazados = rech;
            this.certificadosEmitidos = emitidos;
            this.noCumplenCriterios = noAplican;
            this.ofertaCerrada = cerrada;
        }
    }
}
