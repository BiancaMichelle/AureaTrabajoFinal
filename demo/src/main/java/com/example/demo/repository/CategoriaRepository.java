package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    // Opcional: Para contar ofertas por categoría
    @Query("SELECT COUNT(o) FROM OfertaAcademica o JOIN o.categorias c WHERE c.idCategoria = :categoriaId")
    Optional<Long> countOfertasByCategoriaId(@Param("categoriaId") Long categoriaId);
    
    // Encontrar categorías por nombre exacto (case insensitive)
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}