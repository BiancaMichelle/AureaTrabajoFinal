package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoCertificacion;
import com.example.demo.model.Certificacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;

@Repository
public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {
    
    /**
     * Busca la certificación de una inscripción específica
     */
    Optional<Certificacion> findByInscripcion(Inscripciones inscripcion);
    
    /**
     * Obtiene todas las certificaciones de una oferta académica
     */
    @Query("SELECT c FROM Certificacion c WHERE c.inscripcion.oferta = :oferta")
    List<Certificacion> findByOferta(@Param("oferta") OfertaAcademica oferta);
    
    /**
     * Obtiene certificaciones por estado en una oferta
     */
    @Query("SELECT c FROM Certificacion c WHERE c.inscripcion.oferta = :oferta AND c.estado = :estado")
    List<Certificacion> findByOfertaAndEstado(
        @Param("oferta") OfertaAcademica oferta, 
        @Param("estado") EstadoCertificacion estado
    );
    
    /**
     * Cuenta cuántos alumnos están propuestos para certificación
     */
    @Query("SELECT COUNT(c) FROM Certificacion c WHERE c.inscripcion.oferta = :oferta AND c.estado = 'PROPUESTA'")
    Long countPropuestasEnOferta(@Param("oferta") OfertaAcademica oferta);
    
    /**
     * Cuenta cuántos certificados se emitieron en una oferta
     */
    @Query("SELECT COUNT(c) FROM Certificacion c WHERE c.inscripcion.oferta = :oferta AND c.certificadoEmitido = true")
    Long countCertificadosEmitidosEnOferta(@Param("oferta") OfertaAcademica oferta);
    
    /**
     * Verifica si una inscripción ya tiene certificado emitido
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Certificacion c " +
           "WHERE c.inscripcion = :inscripcion AND c.certificadoEmitido = true")
    boolean existeCertificadoEmitido(@Param("inscripcion") Inscripciones inscripcion);
    
    /**
     * Busca certificaciones pendientes de revisión docente
     */
    @Query("SELECT c FROM Certificacion c WHERE c.inscripcion.oferta = :oferta " +
           "AND c.estado IN ('PROPUESTA', 'PENDIENTE') " +
           "ORDER BY c.promedioGeneral DESC")
    List<Certificacion> findPendientesRevisionEnOferta(@Param("oferta") OfertaAcademica oferta);
    
    /**
     * Busca por número de certificado
     */
    Optional<Certificacion> findByNumeroCertificado(String numeroCertificado);
}
