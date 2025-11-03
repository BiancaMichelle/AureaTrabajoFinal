package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripciones, Long> {
    
    // Buscar inscripción por alumno y oferta
    Optional<Inscripciones> findByAlumnoAndOferta(Usuario alumno, OfertaAcademica oferta);
    
    // Buscar todas las inscripciones de un alumno
    List<Inscripciones> findByAlumno(Usuario alumno);
    
    // Contar inscripciones activas por oferta
    int countByOfertaAndEstadoInscripcionTrue(OfertaAcademica oferta);
}