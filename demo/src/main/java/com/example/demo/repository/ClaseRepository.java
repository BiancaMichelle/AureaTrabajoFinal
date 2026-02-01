package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Clase;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, UUID> {
    
    Optional<Clase> findById(UUID idClase);
    
    List<Clase> findByDocente_DniAndInicioAfterOrderByInicioAsc(String dni, LocalDateTime inicio);

    @Query("SELECT c FROM Clase c LEFT JOIN c.modulo m LEFT JOIN m.curso mc WHERE (c.curso.idOferta IN :cursoIds OR mc.idOferta IN :cursoIds) AND c.inicio > :fecha ORDER BY c.inicio ASC")
    List<Clase> findProximasClasesAlumnos(@Param("cursoIds") List<Long> cursoIds, @Param("fecha") LocalDateTime fecha);

    @Query("SELECT c FROM Clase c LEFT JOIN c.modulo m LEFT JOIN m.curso mc WHERE (c.curso.idOferta IN :cursoIds OR mc.idOferta IN :cursoIds) AND c.inicio BETWEEN :fechaInicio AND :fechaFin ORDER BY c.inicio ASC")
    List<Clase> findClasesCalendario(@Param("cursoIds") List<Long> cursoIds, @Param("fechaInicio") LocalDateTime fechaInicio, @Param("fechaFin") LocalDateTime fechaFin);

    List<Clase> findByDocente_DniAndInicioBetweenOrderByInicioAsc(String dni, LocalDateTime inicio, LocalDateTime fin);

    // Métodos adicionales que podrías necesitar
    List<Clase> findByModuloIdModulo(UUID moduloId);
    List<Clase> findByDocenteDni(String dniDocente);
    List<Clase> findByModuloCursoIdOferta(Long cursoId);
}