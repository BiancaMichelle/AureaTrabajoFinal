package com.example.demo.repository;

import com.example.demo.model.Instituto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutoRepository extends JpaRepository<Instituto, Long> {
    
    /**
     * Obtiene la primera configuración del instituto.
     * Asumimos que solo habrá una configuración por instituto.
     */
    @Query("SELECT i FROM Instituto i ORDER BY i.idInstituto ASC LIMIT 1")
    Optional<Instituto> findFirstInstituto();

    java.util.List<Instituto> findByNombreInstituto(String nombreInstituto);
    
    /**
     * Método alternativo para obtener el primer instituto
     */
    Optional<Instituto> findTopByOrderByIdInstitutoAsc();
}