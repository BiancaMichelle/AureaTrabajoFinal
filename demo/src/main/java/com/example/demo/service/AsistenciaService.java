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
import com.example.demo.repository.UsuarioRepository;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OfertaAcademicaRepository ofertaRepository;
    private final ClaseRepository claseRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, 
                             UsuarioRepository usuarioRepository,
                             OfertaAcademicaRepository ofertaRepository,
                             ClaseRepository claseRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ofertaRepository = ofertaRepository;
        this.claseRepository = claseRepository;
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
            Usuario alumno = usuarioRepository.findByDni(alumnoDni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            OfertaAcademica oferta = ofertaRepository.findById(ofertaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            asistencia.setAlumno(alumno);
            asistencia.setOferta(oferta);
            asistencia.setFecha(claseSeleccionada != null && claseSeleccionada.getInicio() != null
                    ? claseSeleccionada.getInicio().toLocalDate()
                    : fecha);
        }
        
        asistencia.setEstado(estado);
        asistencia.setHora(claseSeleccionada != null && claseSeleccionada.getInicio() != null
                ? claseSeleccionada.getInicio().toLocalTime()
                : LocalTime.now());
        asistencia.setClase(claseSeleccionada);
        
        return asistenciaRepository.save(asistencia);
    }
}
