package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Instituto;

import java.util.List;

@Repository
public interface CarruselImagenRepository extends JpaRepository<CarruselImagen, Long> {
    List<CarruselImagen> findByInstitutoAndActivaTrueOrderByOrden(Instituto instituto);
    List<CarruselImagen> findByInstitutoOrderByOrden(Instituto instituto);
    CarruselImagen findTopByInstitutoOrderByOrdenDesc(Instituto instituto);
}