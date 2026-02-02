package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Asistencia;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {
    List<Asistencia> findByOfertaIdOfertaAndAlumnoDni(Long ofertaId, String alumnoDni);
    Optional<Asistencia> findByOfertaIdOfertaAndAlumnoDniAndFecha(Long ofertaId, String alumnoDni, LocalDate fecha);
    Optional<Asistencia> findByOfertaIdOfertaAndAlumnoDniAndClaseIdClase(Long ofertaId, String alumnoDni, UUID idClase);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.fecha) FROM Asistencia a WHERE a.oferta.idOferta = :ofertaId")
    long countDiasConAsistencia(Long ofertaId);

    List<Asistencia> findByOfertaIdOfertaAndClaseIdClase(Long ofertaId, UUID claseId);
    
    List<Asistencia> findByOfertaIdOfertaAndFecha(Long ofertaId, LocalDate fecha);
    
    List<Asistencia> findByOfertaIdOferta(Long ofertaId);
}
