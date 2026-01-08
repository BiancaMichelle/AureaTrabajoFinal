package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Material;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByCarpetaIdActividad(Long carpetaId);
    
    // Buscar materiales por ID de oferta (curso) a través de la relación Modulo -> Curso
    List<Material> findByModulo_Curso_IdOferta(Long ofertaId);
}
