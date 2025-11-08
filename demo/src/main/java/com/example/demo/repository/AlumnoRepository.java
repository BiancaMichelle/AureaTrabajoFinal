package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Alumno;

public interface AlumnoRepository extends JpaRepository<Alumno,Long> {
    boolean existsByDni(String dni);
    boolean existsByCorreo(String correo);
    Optional<Alumno> findByDni(String dni);
}
