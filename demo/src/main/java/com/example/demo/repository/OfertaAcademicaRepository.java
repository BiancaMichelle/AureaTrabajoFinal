package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.OfertaAcademica;
import com.example.demo.enums.EstadoOferta;

@Repository
public interface OfertaAcademicaRepository extends JpaRepository<OfertaAcademica, Long> {
    
    // Contar ofertas por estado
    long countByEstado(EstadoOferta estado);
    
    // Buscar ofertas por estado
    List<OfertaAcademica> findByEstado(EstadoOferta estado);
}
