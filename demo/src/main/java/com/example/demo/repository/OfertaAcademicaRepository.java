package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.OfertaAcademica;

@Repository
public interface OfertaAcademicaRepository extends JpaRepository<OfertaAcademica, Long> {
    
}
