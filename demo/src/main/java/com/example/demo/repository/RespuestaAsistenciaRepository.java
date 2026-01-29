package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.RespuestaAsistencia;

@Repository
public interface RespuestaAsistenciaRepository extends JpaRepository<RespuestaAsistencia, UUID> {
    
    List<RespuestaAsistencia> findByClaseIdClaseAndAlumnoDni(UUID claseId, String alumnoDni);
    
    @Query("SELECT COUNT(r) FROM RespuestaAsistencia r WHERE r.clase.idClase = :claseId AND r.alumno.dni = :alumnoDni")
    Integer contarRespuestasPorClaseYAlumno(@Param("claseId") UUID claseId, @Param("alumnoDni") String alumnoDni);

    boolean existsByClaseIdClaseAndAlumnoDniAndRonda(UUID claseId, String alumnoDni, Integer ronda);
}
