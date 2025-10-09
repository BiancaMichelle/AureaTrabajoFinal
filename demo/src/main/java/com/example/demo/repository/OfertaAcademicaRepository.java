package com.example.demo.repository;

import com.example.demo.model.OfertaAcademica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfertaAcademicaRepository extends JpaRepository<OfertaAcademica, Long> {
    // Los métodos de filtrado se manejan en el servicio usando streams
}
