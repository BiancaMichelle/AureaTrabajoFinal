package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Charla;

@Repository
public interface CharlaRepository extends JpaRepository<Charla, Long> {
    List<Charla> findByEstado(EstadoOferta estado);
    List<Charla> findByNombreContainingIgnoreCase(String nombre);
}
