package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Clase;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    List<Clase> findByCurso(Curso curso);
    List<Clase> findByDocente(Docente docente);
}
