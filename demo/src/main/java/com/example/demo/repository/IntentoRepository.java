package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Intento;
import com.example.demo.enums.EstadoIntento;

@Repository
public interface IntentoRepository extends JpaRepository<Intento, UUID> {
    List<Intento> findByAlumno(com.example.demo.model.Usuario alumno);
    List<Intento> findByAlumno_IdAndExamen_IdActividad(UUID alumnoId, Long examenId);
    List<Intento> findByExamen_IdActividad(Long examenId);
    Optional<Intento> findByIdIntentoAndExamen_IdActividad(UUID idIntento, Long examenId);
    long countByExamen_IdActividadAndEstado(Long examenId, EstadoIntento estado);
    
    @Query("SELECT i FROM Intento i WHERE i.examen.idActividad IN :examenIds AND i.estado = :estado")
    List<Intento> findByExamenIdsAndEstado(@Param("examenIds") List<Long> examenIds, @Param("estado") EstadoIntento estado);
}
