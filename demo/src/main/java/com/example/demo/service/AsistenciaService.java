package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.Asistencia;
import com.example.demo.model.Clase;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.ClaseRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OfertaAcademicaRepository ofertaRepository;
    private final ClaseRepository claseRepository;
    private final InscripcionRepository inscripcionRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, 
                             UsuarioRepository usuarioRepository,
                             OfertaAcademicaRepository ofertaRepository,
                             ClaseRepository claseRepository,
                             InscripcionRepository inscripcionRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ofertaRepository = ofertaRepository;
        this.claseRepository = claseRepository;
        this.inscripcionRepository = inscripcionRepository;
    }
    
    public AsistenciaRepository getAsistenciaRepository() {
        return asistenciaRepository;
    }

    public List<Asistencia> getAsistenciasPorClase(Long ofertaId, UUID claseId) {
        return asistenciaRepository.findByOfertaIdOfertaAndClaseIdClase(ofertaId, claseId);
    }

    public List<Asistencia> getAsistenciasPorFecha(Long ofertaId, LocalDate fecha) {
        return asistenciaRepository.findByOfertaIdOfertaAndFecha(ofertaId, fecha);
    }
    
    public List<Asistencia> getAsistenciasPorAlumnoYOferta(Long ofertaId, String alumnoDni) {
        return asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(ofertaId, alumnoDni);
    }

    public Asistencia registrarAsistencia(Long ofertaId, String alumnoDni, LocalDate fecha, EstadoAsistencia estado) {
        return registrarAsistencia(ofertaId, alumnoDni, fecha, estado, null);
    }

    public Asistencia registrarAsistencia(Long ofertaId, String alumnoDni, LocalDate fecha, EstadoAsistencia estado, UUID claseId) {
        Long ofertaIdSeguro = Objects.requireNonNull(ofertaId, "El id de la oferta es requerido");
        Optional<Asistencia> existente;
        Clase claseSeleccionada = null;

        if (claseId != null) {
            claseSeleccionada = claseRepository.findById(claseId)
                    .orElseThrow(() -> new RuntimeException("Clase no encontrada"));
            existente = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDniAndClaseIdClase(ofertaIdSeguro, alumnoDni, claseId);
        } else {
            LocalDate fechaSegura = Objects.requireNonNull(fecha, "La fecha es requerida");
            existente = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDniAndFecha(ofertaIdSeguro, alumnoDni, fechaSegura);
        }
        
        Asistencia asistencia;
        if (existente.isPresent()) {
            asistencia = existente.get();
        } else {
            asistencia = new Asistencia();

            // Datos necesarios
            Usuario alumno = usuarioRepository.findByDni(alumnoDni)
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            // Validaciones en el modelo (doble validación desde el servicio)
            LocalDate fechaEfectiva = claseSeleccionada != null && claseSeleccionada.getInicio() != null
                ? claseSeleccionada.getInicio().toLocalDate()
                : fecha;

            // Obtener inscripción activa para fecha de inscripción
            var insOpt = inscripcionRepository.findByAlumnoAndOferta(alumno, oferta);
            if (insOpt.isEmpty() || !Boolean.TRUE.equals(insOpt.get().getEstadoInscripcion())) {
            throw new IllegalArgumentException("El alumno no tiene una inscripción activa en esta oferta");
            }
            LocalDate fechaInscripcion = insOpt.get().getFechaInscripcion();

            // Reglas del modelo
            // Validaciones detalladas para devolver mensajes claros
            LocalDate hoy = LocalDate.now();
            if (fechaEfectiva.isAfter(hoy)) {
                throw new IllegalArgumentException("No se puede registrar asistencia en fechas futuras");
            }
            if (oferta.getFechaInicio() != null && fechaEfectiva.isBefore(oferta.getFechaInicio())) {
                throw new IllegalArgumentException("No se puede registrar asistencia antes del inicio de la oferta");
            }
            if (oferta.getFechaFin() != null && fechaEfectiva.isAfter(oferta.getFechaFin())) {
                throw new IllegalArgumentException("No se puede registrar asistencia después de la fecha de fin de la oferta");
            }
            if (fechaInscripcion != null && fechaEfectiva.isBefore(fechaInscripcion)) {
                throw new IllegalArgumentException("No se puede registrar asistencia antes de la fecha de inscripción del alumno");
            }
            if (claseSeleccionada == null && !oferta.esDiaDeClase(fechaEfectiva)) {
                throw new IllegalArgumentException("El día seleccionado no coincide con los horarios de la oferta");
            }

            asistencia.setAlumno(alumno);
            asistencia.setOferta(oferta);
            asistencia.setFecha(fechaEfectiva);
        }
        
        asistencia.setEstado(estado);
        asistencia.setHora(claseSeleccionada != null && claseSeleccionada.getInicio() != null
                ? claseSeleccionada.getInicio().toLocalTime()
                : LocalTime.now());
        asistencia.setClase(claseSeleccionada);
        
        return asistenciaRepository.save(asistencia);
    }
}
