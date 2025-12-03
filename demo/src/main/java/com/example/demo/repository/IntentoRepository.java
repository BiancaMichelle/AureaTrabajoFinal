package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Intento;

@Repository
public interface IntentoRepository extends JpaRepository<Intento, UUID> {
    List<Intento> findByAlumno_IdAndExamen_IdActividad(UUID alumnoId, Long examenId);
    List<Intento> findByExamen_IdActividad(Long examenId);
    Optional<Intento> findByIdIntentoAndExamen_IdActividad(UUID idIntento, Long examenId);
}
