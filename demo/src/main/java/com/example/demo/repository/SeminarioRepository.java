package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Seminario;

@Repository
public interface SeminarioRepository extends JpaRepository<Seminario, Long> {
    List<Seminario> findByEstado(EstadoOferta estado);
    List<Seminario> findByNombreContainingIgnoreCase(String nombre);
}
