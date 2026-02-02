package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.Notificacion;
import com.example.demo.model.Usuario;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioOrderByFechaDesc(Usuario usuario);
    long countByUsuarioAndLeidaFalse(Usuario usuario);
    List<Notificacion> findByUsuarioAndLeidaFalseAndTipo(Usuario usuario, String tipo);
}
