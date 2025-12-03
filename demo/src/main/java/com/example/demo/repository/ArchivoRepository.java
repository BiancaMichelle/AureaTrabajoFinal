package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Archivo;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {
    List<Archivo> findByCarpetaIdActividad(Long carpetaId);
    List<Archivo> findByMaterialIdActividad(Long materialId);
}
