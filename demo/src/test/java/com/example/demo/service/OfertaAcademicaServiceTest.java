package com.example.demo.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.enums.EstadoIntento;
import com.example.demo.model.*;
import com.example.demo.repository.*;

@ExtendWith(MockitoExtension.class)
public class OfertaAcademicaServiceTest {

    @InjectMocks
    private OfertaAcademicaService ofertaAcademicaService;

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private FormacionRepository formacionRepository;
    @Mock
    private CharlaRepository charlaRepository;
    @Mock
    private SeminarioRepository seminarioRepository;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private ExamenRepository examenRepository;
    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private IntentoRepository intentoRepository;

    @Test
    public void testCalcularProgreso_CursoConActividades() {
        // Setup
        UUID alumnoId = UUID.randomUUID();
        Usuario alumno = new Alumno();
        alumno.setId(alumnoId);

        Long cursoId = 1L;
        Curso curso = new Curso();
        curso.setIdOferta(cursoId);
        curso.setFechaInicio(LocalDate.now().minusDays(10));
        curso.setFechaFin(LocalDate.now().plusDays(10)); // Total 20 days, passed 10. Time prog = 0.5.

        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(curso));
        
        // Mock Tareas
        List<Tarea> tareas = new ArrayList<>();
        Tarea t1 = new Tarea(); t1.setIdActividad(101L);
        tareas.add(t1);
        when(tareaRepository.findByCursoId(cursoId)).thenReturn(tareas);

        // Mock Examenes
        List<Examen> examenes = new ArrayList<>();
        Examen e1 = new Examen(); e1.setIdActividad(201L);
        examenes.add(e1);
        when(examenRepository.findByModulo_Curso_IdOferta(cursoId)).thenReturn(examenes);

        // Mock Entrega (Tarea completed)
        Entrega entrega = new Entrega();
        entrega.setTarea(t1);
        when(entregaRepository.findByEstudiante(alumno)).thenReturn(List.of(entrega));

        // Mock Intento (Examen completed)
        Intento intento = new Intento();
        intento.setEstado(EstadoIntento.FINALIZADO);
        when(intentoRepository.findByAlumno_IdAndExamen_IdActividad(alumnoId, 201L)).thenReturn(List.of(intento));

        // Execute
        // Time Progress: 0.5 (10/20 days)
        // Activity Progress: 1.0 (2/2 activities)
        // Total: (0.5 * 0.5) + (1.0 * 0.5) = 0.25 + 0.5 = 0.75 -> 75%
        
        double progreso = ofertaAcademicaService.calcularProgreso(cursoId, alumno);

        System.out.println("Test Progreso Result: " + progreso);
        assertEquals(75.0, progreso, 0.01);
    }

     @Test
    public void testCalcularProgreso_CursoSinActividades() {
        // Setup
        UUID alumnoId = UUID.randomUUID();
        Usuario alumno = new Alumno();
        alumno.setId(alumnoId);

        Long cursoId = 1L;
        Curso curso = new Curso();
        curso.setIdOferta(cursoId);
        curso.setFechaInicio(LocalDate.now().minusDays(10)); // 10 days ago
        curso.setFechaFin(LocalDate.now().plusDays(10));    // 10 days future. Total 20. 
        
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(curso));
        when(tareaRepository.findByCursoId(cursoId)).thenReturn(List.of());
        when(examenRepository.findByModulo_Curso_IdOferta(cursoId)).thenReturn(List.of());

        // Execute
        // Time Progress: 0.5 -> 50%
        // Activities: 0.
        // Rule: If totalActividades == 0 => return timeProgress * 100
        
        double progreso = ofertaAcademicaService.calcularProgreso(cursoId, alumno);

        System.out.println("Test Progreso Result: " + progreso);
        assertEquals(50.0, progreso, 0.01);
    }
}
