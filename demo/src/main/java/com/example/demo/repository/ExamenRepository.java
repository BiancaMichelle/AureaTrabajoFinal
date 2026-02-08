package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Examen;
import com.example.demo.model.OfertaAcademica;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {
    List<Examen> findByModulo_CursoInAndFechaAperturaBetween(List<OfertaAcademica> ofertas, LocalDateTime start,
            LocalDateTime end);

    List<Examen> findByModulo_CursoIn(List<OfertaAcademica> ofertas);

    List<Examen> findByModulo_Curso_IdOferta(Long idOferta);

    // Buscar pre-exámenes ocultos generados automáticamente por módulo
    List<Examen> findByModulo_IdModuloAndVisibilidadAndGenerarPreExamen(java.util.UUID moduloId, Boolean visibilidad,
            Boolean generarPreExamen);

    // Buscar preexámenes ocultos generados automáticamente por oferta
    List<Examen> findByModulo_Curso_IdOfertaAndVisibilidadAndGenerarPreExamen(Long ofertaId, Boolean visibilidad,
            Boolean generarPreExamen);
}
