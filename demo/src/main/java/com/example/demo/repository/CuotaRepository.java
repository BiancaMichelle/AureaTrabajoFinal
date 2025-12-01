package com.example.demo.repository;

import com.example.demo.model.Cuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    
    List<Cuota> findByInscripcionIdInscripcion(Long inscripcionId);
    
    List<Cuota> findByEstado(String estado);
    
    @Query("SELECT c FROM Cuota c WHERE c.fechaVencimiento < :fecha AND c.estado = 'PENDIENTE'")
    List<Cuota> findCuotasVencidas(@Param("fecha") LocalDate fecha);
    
    @Query("SELECT c FROM Cuota c JOIN c.inscripcion i JOIN i.alumno a WHERE a.id = :usuarioId")
    List<Cuota> findByUsuarioId(@Param("usuarioId") java.util.UUID usuarioId);

    List<Cuota> findByPagoIdPago(Long pagoId);
    
    @Query("SELECT c FROM Cuota c WHERE c.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin")
    List<Cuota> findCuotasEnRangoFecha(@Param("fechaInicio") LocalDate fechaInicio, 
                                       @Param("fechaFin") LocalDate fechaFin);
}