package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Clase;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, UUID> {
    
    Optional<Clase> findById(UUID idClase);
    
    // Métodos adicionales que podrías necesitar
    List<Clase> findByModuloIdModulo(UUID moduloId);
    List<Clase> findByDocenteDni(String dniDocente);
    List<Clase> findByModuloCursoIdOferta(Long cursoId);
}