package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.Formacion;

@Repository
public interface FormacionRepository extends JpaRepository<Formacion, Long> {
    List<Formacion> findByEstado(EstadoOferta estado);
    List<Formacion> findByNombreContainingIgnoreCase(String nombre);
    
    @Query("SELECT f FROM Formacion f JOIN f.docentes d WHERE d.id = :docenteId")
    List<Formacion> findByDocentesId(@Param("docenteId") UUID docenteId);
}
