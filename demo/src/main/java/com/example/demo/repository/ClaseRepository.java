package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Clase;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Modulo;

public interface ClaseRepository extends JpaRepository<Clase, UUID> {
    List<Clase> findByCurso(Curso curso);
    List<Clase> findByModuloOrderByInicioAsc(Modulo modulo);
    List<Clase> findByDocente(Docente docente);
}
