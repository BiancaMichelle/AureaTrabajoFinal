package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, UUID> {
    // Método para buscar módulos por curso ordenados por fecha de inicio (soporta Curso y Formación)
    List<Modulo> findByCursoOrderByFechaInicioModuloAsc(OfertaAcademica curso);
    
    // Métodos adicionales útiles
    List<Modulo> findByCursoAndVisibilidadTrueOrderByFechaInicioModuloAsc(OfertaAcademica curso);
    
    Optional<Modulo> findByIdModulo(UUID idModulo);
    
    @Query("SELECT m FROM Modulo m WHERE m.curso = :curso AND m.visibilidad = true ORDER BY m.fechaInicioModulo ASC")
    List<Modulo> findModulosVisiblesPorCurso(@Param("curso") OfertaAcademica curso);
    
    // Puedes agregar este método temporal para debug
    @Query("SELECT m FROM Modulo m WHERE m.curso.idOferta = :cursoId")
    List<Modulo> findByCursoId(@Param("cursoId") Long cursoId);
}