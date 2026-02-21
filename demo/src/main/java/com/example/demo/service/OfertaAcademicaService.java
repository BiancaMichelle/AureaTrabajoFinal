package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.event.OfertaFinalizadaEvent;
import com.example.demo.model.Auditable;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.SeminarioRepository;
import com.example.demo.model.Usuario;
import com.example.demo.repository.TareaRepository;
import com.example.demo.repository.ExamenRepository;
import com.example.demo.repository.EntregaRepository;
import com.example.demo.repository.IntentoRepository;

@Service
public class OfertaAcademicaService {
    @Autowired
    private CursoRepository cursoRepository;
    
    @Autowired
    private FormacionRepository formacionRepository;
    
    @Autowired
    private CharlaRepository charlaRepository;
    
    @Autowired
    private SeminarioRepository seminarioRepository;

    @Autowired
    private TareaRepository tareaRepository;
    
    @Autowired
    private ExamenRepository examenRepository;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private EntregaRepository entregaRepository;
    
    @Autowired
    private IntentoRepository intentoRepository;

    /**
     * Valida reglas de negocio complejas antes de guardar
     */
    public void validarReglasNegocio(OfertaAcademica oferta) {
        // 1. Validar fechas
        if (oferta.getFechaInicio() != null && oferta.getFechaFin() != null) {
            if (oferta.getFechaInicio().isAfter(oferta.getFechaFin())) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
            }
        }

        // 2. Validar docentes/disertantes según tipo
        if (oferta instanceof Curso) {
            Curso curso = (Curso) oferta;
            if (curso.getDocentes() == null || curso.getDocentes().isEmpty()) {
                throw new IllegalArgumentException("El curso debe tener al menos un docente asignado.");
            }
        } else if (oferta instanceof Charla) {
            Charla charla = (Charla) oferta;
            if (charla.getDisertantes() == null || charla.getDisertantes().isEmpty()) {
                throw new IllegalArgumentException("La charla debe tener al menos un disertante.");
            }
        }

        // 3. Validar horarios (si aplica)
        // if (oferta.getHorarios() == null || oferta.getHorarios().isEmpty()) {
        //     throw new IllegalArgumentException("La oferta debe tener horarios definidos.");
        // }
    }

    public List<OfertaAcademica> obtenerTodas() {
        List<OfertaAcademica> todasLasOfertas = new ArrayList<>();
        todasLasOfertas.addAll(cursoRepository.findAll());
        todasLasOfertas.addAll(formacionRepository.findAll());
        todasLasOfertas.addAll(charlaRepository.findAll());
        todasLasOfertas.addAll(seminarioRepository.findAll());
        return todasLasOfertas;
    }
    public OfertaAcademica guardar(OfertaAcademica oferta) {
        validarReglasNegocio(oferta);
        if (oferta instanceof Curso) {
            return cursoRepository.save((Curso) oferta);
        } else if (oferta instanceof Formacion) {
            return formacionRepository.save((Formacion) oferta);
        } else if (oferta instanceof Charla) {
            return charlaRepository.save((Charla) oferta);
        } else if (oferta instanceof Seminario) {
            return seminarioRepository.save((Seminario) oferta);
        }
        throw new IllegalArgumentException("Tipo de oferta no soportado");
    }
    /**
     * Realiza una baja lógica de la oferta (cambia estado a DE_BAJA)
     */
    @Transactional
    public void eliminar(Long id, String tipo) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id, tipo);
        if (ofertaOpt.isPresent()) {
            OfertaAcademica oferta = ofertaOpt.get();
            oferta.setEstado(EstadoOferta.DE_BAJA);
            guardar(oferta);
        }
    }

    /**
     * Obtiene una oferta específica por ID y tipo
     */
    public Optional<OfertaAcademica> obtenerPorId(Long id, String tipo) {
        switch (tipo.toUpperCase()) {
            case "CURSO":
                return cursoRepository.findById(id).map(OfertaAcademica.class::cast);
            case "FORMACION":
                return formacionRepository.findById(id).map(OfertaAcademica.class::cast);
            case "CHARLA":
                return charlaRepository.findById(id).map(OfertaAcademica.class::cast);
            case "SEMINARIO":
                return seminarioRepository.findById(id).map(OfertaAcademica.class::cast);
            default:
                return Optional.empty();
        }
    }

    /**
     * Obtiene una oferta por ID, detectando automáticamente el tipo
     */
    public Optional<OfertaAcademica> obtenerPorId(Long id) {
        // Buscar en todos los repositorios
        Optional<OfertaAcademica> curso = cursoRepository.findById(id).map(OfertaAcademica.class::cast);
        if (curso.isPresent()) return curso;

        Optional<OfertaAcademica> formacion = formacionRepository.findById(id).map(OfertaAcademica.class::cast);
        if (formacion.isPresent()) return formacion;

        Optional<OfertaAcademica> charla = charlaRepository.findById(id).map(OfertaAcademica.class::cast);
        if (charla.isPresent()) return charla;

        return seminarioRepository.findById(id).map(OfertaAcademica.class::cast);
    }

    /**
     * Cambia el estado de una oferta académica
     */
    @Transactional
    public boolean cambiarEstado(Long id, EstadoOferta nuevoEstado) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica oferta = ofertaOpt.get();
        
        // Usar el método del modelo para validar y cambiar estado
        boolean exito = oferta.cambiarEstado(nuevoEstado);
        
        if (exito) {
            guardar(oferta);
        }
        
        return exito;
    }

    /**
     * Da de baja una oferta académica
     */
    @Transactional
    public boolean darDeBaja(Long id) {
        return cambiarEstado(id, EstadoOferta.DE_BAJA);
    }

    /**
     * Da de alta una oferta académica
     */
    @Transactional
    public boolean darDeAlta(Long id) {
        return cambiarEstado(id, EstadoOferta.ACTIVA);
    }

    /**
     * Verifica si una oferta puede ser eliminada
     */
    public boolean puedeEliminar(Long id) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        return ofertaOpt.get().puedeSerEliminada();
    }

    /**
     * Elimina una oferta con validaciones (Baja lógica)
     */
    @Transactional
    public boolean eliminarConValidacion(Long id) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica oferta = ofertaOpt.get();
        
        if (!oferta.puedeSerEliminada()) {
            return false;
        }
        
        // Baja lógica
        oferta.setEstado(EstadoOferta.DE_BAJA);
        guardar(oferta);
        return true;
    }

    /**
     * Modifica una oferta académica
     */
    @Auditable(action = "MODIFICAR", entity = "OfertaAcadémica")
    @Transactional
    public boolean modificar(Long id, OfertaAcademica datosNuevos) {
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(id);
        
        if (!ofertaOpt.isPresent()) {
            return false;
        }
        
        OfertaAcademica ofertaExistente = ofertaOpt.get();
        
        // Verificar si puede ser editada
        if (!ofertaExistente.puedeSerEditada()) {
            return false;
        }
        
        // Validar datos nuevos
        List<String> errores = datosNuevos.validarDatos();
        if (!errores.isEmpty()) {
            return false;
        }
        
        // Aplicar modificaciones básicas
        ofertaExistente.modificarDatosBasicos(
            datosNuevos.getNombre(),
            datosNuevos.getDescripcion(),
            datosNuevos.getDuracion(),
            datosNuevos.getFechaInicio(),
            datosNuevos.getFechaFin(),
            datosNuevos.getModalidad(),
            datosNuevos.getCupos(),
            datosNuevos.getVisibilidad(),
            datosNuevos.getCostoInscripcion(),
            0.0,
            datosNuevos.getCertificado(),
            datosNuevos.getLugar(),
            datosNuevos.getEnlace(),
            datosNuevos.getFechaInicioInscripcion(),
            datosNuevos.getFechaFinInscripcion()
        );
        
        // Aplicar modificaciones específicas según el tipo
        if (ofertaExistente instanceof Curso && datosNuevos instanceof Curso) {
            Curso cursoExistente = (Curso) ofertaExistente;
            Curso cursoNuevo = (Curso) datosNuevos;
            
            cursoExistente.modificarDatosCurso(
                cursoNuevo.getTemario(),
                cursoNuevo.getDocentes(),
                cursoNuevo.getCostoCuota(),
                cursoNuevo.getCostoMora(),
                cursoNuevo.getNrCuotas(),
                cursoNuevo.getDiaVencimiento()
            );
        } else if (ofertaExistente instanceof Formacion && datosNuevos instanceof Formacion) {
            Formacion formacionExistente = (Formacion) ofertaExistente;
            Formacion formacionNueva = (Formacion) datosNuevos;
            
            formacionExistente.modificarDatosFormacion(
                formacionNueva.getPlan(),
                formacionNueva.getDocentes(),
                formacionNueva.getCostoCuota(),
                formacionNueva.getCostoMora(),
                formacionNueva.getNrCuotas(),
                formacionNueva.getDiaVencimiento()
            );
        } else if (ofertaExistente instanceof Charla && datosNuevos instanceof Charla) {
            Charla charlaExistente = (Charla) ofertaExistente;
            Charla charlaNueva = (Charla) datosNuevos;
            
            charlaExistente.modificarDatosCharla(
                charlaNueva.getDuracionEstimada(),
                charlaNueva.getDisertantes(),
                charlaNueva.getPublicoObjetivo()
            );
        } else if (ofertaExistente instanceof Seminario && datosNuevos instanceof Seminario) {
            Seminario seminarioExistente = (Seminario) ofertaExistente;
            Seminario seminarioNuevo = (Seminario) datosNuevos;
            
            seminarioExistente.modificarDatosSeminario(
                seminarioNuevo.getPublicoObjetivo(),
                seminarioNuevo.getDuracionMinutos(),
                seminarioNuevo.getDisertantes()
            );
        }
        
        // Guardar cambios
        guardar(ofertaExistente);
        return true;
    }

    /**
     * Obtiene ofertas por estado
     */
    public List<OfertaAcademica> obtenerPorEstado(EstadoOferta estado) {
        List<OfertaAcademica> ofertas = new ArrayList<>();
        ofertas.addAll(cursoRepository.findByEstado(estado));
        ofertas.addAll(formacionRepository.findByEstado(estado));
        ofertas.addAll(charlaRepository.findByEstado(estado));
        ofertas.addAll(seminarioRepository.findByEstado(estado));
        return ofertas;
    }

    /**
     * Busca ofertas por nombre
     */
    public List<OfertaAcademica> buscarPorNombre(String nombre) {
        List<OfertaAcademica> ofertas = new ArrayList<>();
        ofertas.addAll(cursoRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(formacionRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(charlaRepository.findByNombreContainingIgnoreCase(nombre));
        ofertas.addAll(seminarioRepository.findByNombreContainingIgnoreCase(nombre));
        return ofertas;
    }

    /**
     * Tarea programada para verificar y actualizar estados de ofertas finalizadas
     * Se ejecuta cada 10 minutos (600000 ms)
     */
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void verificarOfertasFinalizadas() {
        List<OfertaAcademica> todas = obtenerTodas();
        int actualizadas = 0;
        List<OfertaAcademica> recienFinalizadas = new ArrayList<>();
        
        for (OfertaAcademica oferta : todas) {
            try {
                EstadoOferta estadoAnterior = oferta.getEstado();
                
                if (oferta.actualizarEstadoSiFinalizada()) {
                    guardar(oferta);
                    actualizadas++;
                    
                    // Si cambió a FINALIZADA, publicar evento
                    if (oferta.getEstado() == EstadoOferta.FINALIZADA && 
                        estadoAnterior != EstadoOferta.FINALIZADA) {
                        recienFinalizadas.add(oferta);
                        System.out.println("🎓 Oferta finalizada detectada: " + oferta.getNombre());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al actualizar estado de la oferta ID: " + oferta.getIdOferta() + " (" + oferta.getNombre() + "): " + e.getMessage());
            }
        }
        
        if (actualizadas > 0) {
            System.out.println("🔄 Se actualizaron " + actualizadas + " ofertas (FINALIZADA/EN CURSO)");
        }
        
        // Publicar eventos para ofertas recién finalizadas
        for (OfertaAcademica oferta : recienFinalizadas) {
            try {
                System.out.println("📢 Publicando evento de finalización para: " + oferta.getNombre());
                eventPublisher.publishEvent(new OfertaFinalizadaEvent(this, oferta));
            } catch (Exception e) {
                System.err.println("❌ Error al publicar evento: " + e.getMessage());
            }
        }
    }

    /**
     * Calcula el progreso de un alumno en una oferta académica.
     * Basado en tiempo transcurrido (50%) y actividades completadas (50%).
     */
    @Transactional(readOnly = true)
    public double calcularProgreso(Long ofertaId, Usuario alumno) {
        System.out.println("DEBUG PROGRESO: Calculando para oferta " + ofertaId + " alumno " + alumno.getId());
        Optional<OfertaAcademica> ofertaOpt = obtenerPorId(ofertaId);
        if (ofertaOpt.isEmpty()) {
            System.out.println("DEBUG PROGRESO: Oferta no encontrada");
            return 0.0;
        }
        OfertaAcademica oferta = ofertaOpt.get();

        // 1. Progreso temporal
        double timeProgress = 0.0;
        long diasTotal = 0;
        long diasTranscurridos = 0;
        
        if (oferta.getFechaInicio() != null && oferta.getFechaFin() != null) {
            java.time.LocalDate hoy = java.time.LocalDate.now();
            if (!hoy.isBefore(oferta.getFechaInicio())) {
                 diasTotal = java.time.temporal.ChronoUnit.DAYS.between(oferta.getFechaInicio(), oferta.getFechaFin());
                 diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(oferta.getFechaInicio(), hoy);
                 if (diasTotal > 0) {
                      timeProgress = (double) diasTranscurridos / diasTotal;
                 }
                 if (timeProgress < 0) timeProgress = 0.0;
                 if (timeProgress > 1) timeProgress = 1.0;
            }
        }
        System.out.println("DEBUG PROGRESO: TimeProgress=" + timeProgress + " (TotalDias=" + diasTotal + ", Transcurridos=" + diasTranscurridos + ")");
        
        // 2. Progreso actividades
        List<com.example.demo.model.Tarea> tareas = tareaRepository.findByCursoId(ofertaId);
        List<com.example.demo.model.Examen> examenes = examenRepository.findByModulo_Curso_IdOferta(ofertaId);
        
        long totalActividades = tareas.size() + examenes.size();
        System.out.println("DEBUG PROGRESO: Total Actividades=" + totalActividades + " (Tareas=" + tareas.size() + ", Examenes=" + examenes.size() + ")");
        
        // Optimización: Usar IDs para evitar lazy loading traversal
        java.util.Set<Long> tareaIds = tareas.stream().map(t -> t.getIdActividad()).collect(java.util.stream.Collectors.toSet());
        
        long entregasCompletas = 0;
        if (!tareas.isEmpty()) {
             // Traer todas las entregas del alumno y filtrar en memoria por IDs de tareas del curso
             entregasCompletas = entregaRepository.findByEstudiante(alumno).stream()
                 .filter(e -> tareaIds.contains(e.getTarea().getIdActividad()))
                 .count();
        }
        System.out.println("DEBUG PROGRESO: Entregas Completas=" + entregasCompletas);
        
        long examenesRendidos = 0;
        if (!examenes.isEmpty()) {
            for(com.example.demo.model.Examen e : examenes) {
                // Verificar si hay intento finalizado para este examen
                java.util.List<com.example.demo.model.Intento> intentos = intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumno.getId(), e.getIdActividad());
                boolean finalizado = intentos.stream().anyMatch(i -> i.getEstado() == com.example.demo.enums.EstadoIntento.FINALIZADO);
                if(finalizado) examenesRendidos++;
                
                System.out.println("DEBUG PROGRESO: Examen " + e.getIdActividad() + " Intentos=" + intentos.size() + " Finalizado=" + finalizado);
                if (!intentos.isEmpty()) {
                    intentos.forEach(i -> System.out.println("   Intento State: " + i.getEstado()));
                }
            }
        }
        System.out.println("DEBUG PROGRESO: Examenes Rendidos=" + examenesRendidos);

        double activityProgress = 0.0;
        if (totalActividades > 0) {
            activityProgress = (double) (entregasCompletas + examenesRendidos) / totalActividades;
             if (activityProgress > 1) activityProgress = 1.0;
        }
        System.out.println("DEBUG PROGRESO: ActivityProgress=" + activityProgress);

        // 3. Ponderación (50% tiempo, 50% actividades)
        if (totalActividades == 0) {
            return Math.round(timeProgress * 100.0);
        }

        double total = (timeProgress * 0.5 + activityProgress * 0.5) * 100.0;
        System.out.println("DEBUG PROGRESO: Total Calc=" + total);
        return Math.round(total * 100.0) / 100.0;
    }

    
}
