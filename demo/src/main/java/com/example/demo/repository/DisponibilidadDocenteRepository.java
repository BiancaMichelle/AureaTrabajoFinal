package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.Dias;
import com.example.demo.model.Docente;
import com.example.demo.model.DisponibilidadDocente;

@Repository
public interface DisponibilidadDocenteRepository extends JpaRepository<DisponibilidadDocente, Long> {
    
    /**
     * Busca todas las disponibilidades de un docente
     */
    List<DisponibilidadDocente> findByDocente(Docente docente);
    
    /**
     * Busca todas las disponibilidades de un docente ordenadas por día y hora
     */
    @Query("SELECT d FROM DisponibilidadDocente d WHERE d.docente = :docente ORDER BY d.dia, d.horaInicio")
    List<DisponibilidadDocente> findByDocenteOrderByDiaAndHora(@Param("docente") Docente docente);
    
    /**
     * Busca disponibilidades de un docente en un día específico
     */
    List<DisponibilidadDocente> findByDocenteAndDia(Docente docente, Dias dia);
    
    /**
     * Elimina todas las disponibilidades de un docente
     */
    void deleteByDocente(Docente docente);
    
    /**
     * Cuenta cuántas disponibilidades tiene un docente
     */
    long countByDocente(Docente docente);
}
