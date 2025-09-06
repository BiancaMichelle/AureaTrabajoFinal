package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.InstitucionAlumno;
import com.example.demo.repository.InstitucionRepository;

@Service
public class InstitucionService {
    private InstitucionRepository institucionRepository;

    public InstitucionService(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    public List<InstitucionAlumno> obtenerTodasLasInstituciones() {
        return institucionRepository.findAll();
    }
}
