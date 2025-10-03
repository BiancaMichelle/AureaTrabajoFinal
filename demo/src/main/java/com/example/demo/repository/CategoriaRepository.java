package com.example.demo.repository;

import com.example.demo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByDescripcionIgnoreCase(String descripcion);
    
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);
}
