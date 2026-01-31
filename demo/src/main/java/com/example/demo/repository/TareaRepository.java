package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Curso;
import com.example.demo.model.Tarea;
import com.example.demo.model.Usuario;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    
    @Query("SELECT t FROM Tarea t " +
           "LEFT JOIN FETCH t.modulo m " +
           "LEFT JOIN FETCH m.curso c " +
           "WHERE t.idActividad = :id")
    Optional<Tarea> findByIdWithModuloAndCurso(@Param("id") Long id);

    @Query("SELECT t FROM Tarea t JOIN t.modulo m WHERE m.curso.idOferta = :cursoId")
    List<Tarea> findByCursoId(@Param("cursoId") Long cursoId);

    List<Tarea> findByModuloCursoIn(List<Curso> cursos);
}
