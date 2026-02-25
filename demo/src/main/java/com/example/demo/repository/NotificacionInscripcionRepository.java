package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.NotificacionInscripcion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;

@Repository
public interface NotificacionInscripcionRepository extends JpaRepository<NotificacionInscripcion, Long> {
    
    List<NotificacionInscripcion> findByNotificadoFalse();
    
    Optional<NotificacionInscripcion> findByUsuarioAndOfertaAndNotificadoFalse(Usuario usuario, OfertaAcademica oferta);
    
    List<NotificacionInscripcion> findByOferta(OfertaAcademica oferta);

    List<NotificacionInscripcion> findByOfertaAndNotificadoFalse(OfertaAcademica oferta);
}
