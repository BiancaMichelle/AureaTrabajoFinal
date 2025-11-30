package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.enums.EstadoAsistencia;
import com.example.demo.model.Asistencia;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OfertaAcademicaRepository ofertaRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, 
                             UsuarioRepository usuarioRepository,
                             OfertaAcademicaRepository ofertaRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ofertaRepository = ofertaRepository;
    }

    public List<Asistencia> getAsistenciasPorAlumnoYOferta(Long ofertaId, String alumnoDni) {
        return asistenciaRepository.findByOfertaIdOfertaAndAlumnoDni(ofertaId, alumnoDni);
    }

    public Asistencia registrarAsistencia(Long ofertaId, String alumnoDni, LocalDate fecha, EstadoAsistencia estado) {
        Optional<Asistencia> existente = asistenciaRepository.findByOfertaIdOfertaAndAlumnoDniAndFecha(ofertaId, alumnoDni, fecha);
        
        Asistencia asistencia;
        if (existente.isPresent()) {
            asistencia = existente.get();
        } else {
            asistencia = new Asistencia();
            Usuario alumno = usuarioRepository.findByDni(alumnoDni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            asistencia.setAlumno(alumno);
            asistencia.setOferta(oferta);
            asistencia.setFecha(fecha);
        }
        
        asistencia.setEstado(estado);
        asistencia.setHora(LocalTime.now());
        
        return asistenciaRepository.save(asistencia);
    }
}
