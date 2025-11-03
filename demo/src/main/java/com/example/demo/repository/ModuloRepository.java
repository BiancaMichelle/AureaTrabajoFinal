package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Curso;
import com.example.demo.model.Modulo;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, UUID> {
    // Método para buscar módulos por curso ordenados por fecha de inicio
    List<Modulo> findByCursoOrderByFechaInicioModuloAsc(Curso curso);
    
    // Métodos adicionales útiles
    List<Modulo> findByCursoAndVisibilidadTrueOrderByFechaInicioModuloAsc(Curso curso);
    
    Optional<Modulo> findByIdModulo(UUID idModulo);
    
    @Query("SELECT m FROM Modulo m WHERE m.curso = :curso AND m.visibilidad = true ORDER BY m.fechaInicioModulo ASC")
    List<Modulo> findModulosVisiblesPorCurso(@Param("curso") Curso curso);
    
    // Puedes agregar este método temporal para debug
    @Query("SELECT m FROM Modulo m WHERE m.curso.idOferta = :cursoId")
    List<Modulo> findByCursoId(@Param("cursoId") Long cursoId);
}