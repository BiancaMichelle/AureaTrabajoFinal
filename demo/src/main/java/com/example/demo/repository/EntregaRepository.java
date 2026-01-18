package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Entrega;
import com.example.demo.model.Tarea;
import com.example.demo.model.Usuario;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    
    Optional<Entrega> findByTareaAndEstudiante(Tarea tarea, Usuario estudiante);
    
    List<Entrega> findByEstudiante(Usuario estudiante);
    
    List<Entrega> findByTarea(Tarea tarea);
}
