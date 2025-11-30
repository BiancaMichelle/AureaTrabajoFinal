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
}
