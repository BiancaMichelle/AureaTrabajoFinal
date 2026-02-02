package com.example.demo.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Examen;
import com.example.demo.model.OfertaAcademica;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {
    List<Examen> findByModulo_CursoInAndFechaAperturaBetween(List<OfertaAcademica> ofertas, LocalDateTime start, LocalDateTime end);
    List<Examen> findByModulo_CursoIn(List<OfertaAcademica> ofertas);
    List<Examen> findByModulo_Curso_IdOferta(Long idOferta);
}
