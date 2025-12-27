package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, UUID> {
    boolean existsByDni(String dni);
    Optional<Docente> findByDni(String dni);
    
    @Query("SELECT d FROM Docente d WHERE " +
           "(LOWER(d.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.matricula) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "EXISTS (SELECT r FROM d.roles r WHERE r.nombre = 'DOCENTE')")
    List<Docente> buscarPorNombreApellidoOMatricula(@Param("query") String query);
    
    @Query("SELECT d FROM Docente d WHERE " +
           "EXISTS (SELECT r FROM d.roles r WHERE r.nombre = 'DOCENTE') " +
           "ORDER BY d.apellido, d.nombre")
    List<Docente> findAllDocentes();
}
