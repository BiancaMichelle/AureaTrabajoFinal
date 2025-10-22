package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Horario;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    
}
