package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Usuario;
public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByDni(String dni);
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByDniOrCorreo(String dni, String correo);
    Optional<Usuario> findByTokenRecuperacion(String token);
    
    // ✅ AGREGAR ESTE MÉTODO
    @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre")
    List<Usuario> findByRolesNombre(@Param("rolNombre") String rolNombre);
}