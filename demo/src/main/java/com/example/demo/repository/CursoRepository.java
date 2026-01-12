package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Curso;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByEstado(EstadoOferta estado);
    List<Curso> findByNombreContainingIgnoreCase(String nombre);
    Optional<Curso> findByIdOferta(Long idOferta);
    @Query("SELECT c FROM Curso c JOIN c.docentes d WHERE d.id = :docenteId")
    List<Curso> findByDocentesId(@Param("docenteId") UUID docenteId);

    // Método derivado estándar de Spring Data JPA (más seguro que @Query manual para boolean)
    long countByDocentesIdAndEstadoIn(UUID docenteId, List<EstadoOferta> estados);
}
