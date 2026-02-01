package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Examen;
import com.example.demo.model.Usuario;
import com.example.demo.model.Curso;
import java.time.LocalDateTime;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {
    List<Examen> findByModulo_CursoInAndFechaAperturaBetween(List<Curso> cursos, LocalDateTime start, LocalDateTime end);
    List<Examen> findByModulo_Curso_IdOferta(Long idOferta);
}
