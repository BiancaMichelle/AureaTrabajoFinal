package com.example.demo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByDni(String dni);
    Optional<Usuario> findByCorreo(String correo);
}
