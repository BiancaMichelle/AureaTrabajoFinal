package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CharlaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.FormacionRepository;
import com.example.demo.repository.SeminarioRepository;

@Service
public class OfertaAcademicaService {
    @Autowired
    private CursoRepository cursoRepository;
    
    @Autowired
    private FormacionRepository formacionRepository;
    
    @Autowired
    private CharlaRepository charlaRepository;
    
    @Autowired
    private SeminarioRepository seminarioRepository;

    public List<OfertaAcademica> obtenerTodas() {
        List<OfertaAcademica> todasLasOfertas = new ArrayList<>();
        todasLasOfertas.addAll(cursoRepository.findAll());
        todasLasOfertas.addAll(formacionRepository.findAll());
        todasLasOfertas.addAll(charlaRepository.findAll());
        todasLasOfertas.addAll(seminarioRepository.findAll());
        return todasLasOfertas;
    }
    public OfertaAcademica guardar(OfertaAcademica oferta) {
        if (oferta instanceof Curso) {
            return cursoRepository.save((Curso) oferta);
        } else if (oferta instanceof Formacion) {
            return formacionRepository.save((Formacion) oferta);
        } else if (oferta instanceof Charla) {
            return charlaRepository.save((Charla) oferta);
        } else if (oferta instanceof Seminario) {
            return seminarioRepository.save((Seminario) oferta);
        }
        throw new IllegalArgumentException("Tipo de oferta no soportado");
    }
    public void eliminar(Long id, String tipo) {
        switch (tipo.toUpperCase()) {
            case "CURSO":
                Long cursoId = Long.valueOf(id);
                cursoRepository.deleteById(cursoId);
                break;
            case "FORMACION":
                formacionRepository.deleteById(id);
                break;
            case "CHARLA":
                charlaRepository.deleteById(id);
                break;
            case "SEMINARIO":
                seminarioRepository.deleteById(id);
                break;
        }
    }
}
