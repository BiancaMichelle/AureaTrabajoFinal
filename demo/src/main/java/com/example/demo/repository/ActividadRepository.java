package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Actividad;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    
}
