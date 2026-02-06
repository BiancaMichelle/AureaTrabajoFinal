package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Docente;
import com.example.demo.model.Horario;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
	void deleteByDocente(Docente docente);
    
    /**
     * Busca todos los horarios asignados de un docente
     */
    List<Horario> findByDocente(Docente docente);
    
    // Check if a docente is assigned to an offer
    boolean existsByOfertaAcademica_IdOfertaAndDocente_Id(Long ofertaId, java.util.UUID docenteId);
}
