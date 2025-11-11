package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.model.Clase;
import com.example.demo.model.Curso;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CursoService {
    
    private final CursoRepository cursoRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final ModuloRepository moduloRepository;
    
    public CursoService(CursoRepository cursoRepository,
                       OfertaAcademicaRepository ofertaAcademicaRepository,
                       ModuloRepository moduloRepository) {
        this.cursoRepository = cursoRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.moduloRepository = moduloRepository;
    }
    
    public Curso obtenerCursoPorId(Long cursoId) {
        return cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));
    }
    
    public Modulo crearModulo(String nombre, String descripcion, String objetivos,
                               LocalDate fechaInicio, LocalDate fechaFin, 
                               Boolean visibilidad, Long cursoId) {
        // ✅ Buscar en OfertaAcademicaRepository para soportar Cursos Y Formaciones
        OfertaAcademica curso = ofertaAcademicaRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Oferta académica no encontrada"));
        
        System.out.println("📚 Creando módulo para: " + curso.getNombre() + " (Tipo: " + curso.getClass().getSimpleName() + ")");
        
        Modulo modulo = new Modulo();
        modulo.setNombre(nombre);
        modulo.setDescripcion(descripcion);
        modulo.setFechaInicioModulo(fechaInicio);
        modulo.setFechaFinModulo(fechaFin);
        modulo.setObjetivos(objetivos != null && !objetivos.trim().isEmpty() ? objetivos : "Objetivos del módulo");
        modulo.setVisibilidad(visibilidad != null ? visibilidad : true);
        modulo.setCurso(curso);
        modulo.setClases(new ArrayList<>());
        modulo.setActividades(new ArrayList<>());
        
        return moduloRepository.save(modulo);
    }
    
    public Clase crearClase(String titulo, String descripcion, UUID moduloId) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
        
        Clase clase = new Clase();
        clase.setTitulo(titulo);
        clase.setDescripcion(descripcion);
        clase.setInicio(LocalDateTime.now().plusDays(1));
        clase.setAsistenciaAutomatica(true);
        clase.setPreguntasAleatorias(false);
        clase.setCantidadPreguntas(0);
        clase.setModulo(modulo);
        clase.setCurso(modulo.getCurso());
        
        // Guardar la clase (necesitarías ClaseRepository)
        // return claseRepository.save(clase);
        return clase;
    }
}