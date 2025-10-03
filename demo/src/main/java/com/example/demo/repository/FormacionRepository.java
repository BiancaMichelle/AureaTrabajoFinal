package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Formacion;

@Repository
public interface FormacionRepository extends JpaRepository<Formacion, Long> {
    List<Formacion> findByEstado(EstadoOferta estado);
    List<Formacion> findByNombreContainingIgnoreCase(String nombre);
}
