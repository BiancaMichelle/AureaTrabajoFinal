package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.TransactionSystemException;

import com.example.demo.enums.Modalidad;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;

@SpringBootTest
public class OfertaValidationTest {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private CharlaRepository charlaRepository;

    @Test
    public void testCursoDebeTenerDocente() {
        Curso curso = new Curso();
        curso.setNombre("Curso Test Sin Docente");
        curso.setDescripcion("Descripcion");
        curso.setFechaInicio(LocalDate.now().plusDays(1));
        curso.setFechaFin(LocalDate.now().plusDays(10));
        curso.setModalidad(Modalidad.VIRTUAL);
        curso.setCostoInscripcion(100.0);
        curso.setCertificado(true);
        curso.setDocentes(new ArrayList<>()); // Lista vacía

        // Esperamos que falle al guardar debido a @PrePersist
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cursoRepository.save(curso);
        });

        assertTrue(exception.getMessage().contains("debe tener al menos un docente"));
    }

    @Test
    public void testCharlaDebeTenerDisertante() {
        Charla charla = new Charla();
        charla.setNombre("Charla Test Sin Disertante");
        charla.setDescripcion("Descripcion");
        charla.setFechaInicio(LocalDate.now().plusDays(1));
        charla.setFechaFin(LocalDate.now().plusDays(1));
        charla.setModalidad(Modalidad.VIRTUAL);
        charla.setCostoInscripcion(0.0);
        charla.setCertificado(false);
        charla.setDisertantes(new ArrayList<>()); // Lista vacía

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            charlaRepository.save(charla);
        });

        assertTrue(exception.getMessage().contains("debe tener al menos un disertante"));
    }

    @Test
    public void testFechasInvalidas() {
        Charla charla = new Charla();
        charla.setNombre("Charla Fechas Mal");
        charla.setDescripcion("Descripcion");
        charla.setFechaInicio(LocalDate.now().plusDays(10));
        charla.setFechaFin(LocalDate.now().plusDays(1)); // FIN ANTES DE INICIO
        charla.setModalidad(Modalidad.VIRTUAL);
        charla.setCostoInscripcion(0.0);
        charla.setCertificado(false);
        
        ArrayList<String> disertantes = new ArrayList<>();
        disertantes.add("Juan");
        charla.setDisertantes(disertantes);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            charlaRepository.save(charla);
        });

        assertTrue(exception.getMessage().contains("no puede ser posterior"));
    }
}
