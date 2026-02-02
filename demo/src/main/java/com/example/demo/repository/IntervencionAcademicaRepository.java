package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.IntervencionAcademica;
import com.example.demo.model.Usuario;
import java.util.List;

@Repository
public interface IntervencionAcademicaRepository extends JpaRepository<IntervencionAcademica, Long> {
    List<IntervencionAcademica> findByAlumno(Usuario alumno);
}
