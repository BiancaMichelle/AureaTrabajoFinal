package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripciones, Long> {
    
    // Buscar inscripción por usuario (alumno/docente) y oferta
    Optional<Inscripciones> findByAlumnoAndOferta(Usuario alumno, OfertaAcademica oferta);
    
    // Buscar todas las inscripciones de un usuario por id del alumno
    List<Inscripciones> findByAlumnoId(UUID alumnoId);

    // Alternativa: buscar por la entidad Usuario cuando se tenga el objeto
    List<Inscripciones> findByAlumno(Usuario alumno);
    
    // Contar inscripciones activas por oferta
    int countByOfertaAndEstadoInscripcionTrue(OfertaAcademica oferta);

    // Contar inscripciones activas por alumno
    int countByAlumnoAndEstadoInscripcionTrue(Usuario alumno);

    // Listar inscripciones activas por oferta
    List<Inscripciones> findByOfertaAndEstadoInscripcionTrue(OfertaAcademica oferta);

    @Query("SELECT i FROM Inscripciones i WHERE i.alumno.dni = :dniAlumno")
    List<Inscripciones> findByAlumnoDni(String dniAlumno);

    @Query("SELECT i FROM Inscripciones i WHERE i.alumno.dni = :dniAlumno AND i.oferta.idOferta = :idOferta")
    List<Inscripciones> findByAlumnoDniAndOfertaId(String dniAlumno, Long idOferta);

    List<Inscripciones> findByOfertaIdOferta(Long ofertaId);
    
    // Contar inscripciones por rango de fecha
    long countByFechaInscripcionBetween(java.time.LocalDate inicio, java.time.LocalDate fin);

    // Contar inscripciones activas (finalizaron) en ofertas cerradas
    long countByOfertaEstadoAndEstadoInscripcionTrue(com.example.demo.enums.EstadoOferta estado);

    // Contar total inscripciones en ofertas cerradas
    long countByOfertaEstado(com.example.demo.enums.EstadoOferta estado);

    boolean existsByAlumnoAndEstadoInscripcionTrue(Usuario alumno);
}
