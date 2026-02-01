package com.example.demo.controller;

import java.util.List;
import java.security.Principal;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.InstitutoService;

import java.util.stream.Collectors;

@Controller
public class PublicoController {
    
    @Autowired
    private InstitutoService institutoService;
    
    @Autowired
    private CarruselImagenRepository carruselImagenRepository;
    
    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private FormacionRepository formacionRepository;
    
    @GetMapping({"/", "/publico"})
    public String publico(Model model, Principal principal) {
        // Obtener datos del instituto
        Instituto instituto = institutoService.obtenerInstituto();
        
        // Obtener imágenes del carrusel
        List<CarruselImagen> imagenesCarrusel = carruselImagenRepository.findByInstitutoAndActivaTrueOrderByOrden(instituto);
        
        // Obtener ofertas académicas filtradas:
        // Solo Estado ACTIVA o ENCURSO (si permite inscripciones o visualización)
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll().stream()
            .filter(o -> o.getEstado() == EstadoOferta.ACTIVA || o.getEstado() == EstadoOferta.ENCURSO)
            .collect(Collectors.toList());

        // Obtener categorías
        List<Categoria> categorias = categoriaRepository.findAll();
        
        // Verificar inscripciones y docencia del usuario logueado
        Set<Long> idsInscritos = new HashSet<>();
        Set<Long> idsDocente = new HashSet<>();

        if (principal != null) {
            String dni = principal.getName();
            usuarioRepository.findByDni(dni).ifPresent(usuario -> {
                // 1. Obtener IDs de ofertas donde está INSCRITO (Como Alumno)
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoId(usuario.getId());
                idsInscritos.addAll(inscripciones.stream()
                        .map(i -> i.getOferta().getIdOferta())
                        .collect(Collectors.toSet()));

                // 2. Obtener IDs de ofertas donde es DOCENTE
                // Cursos
                List<Curso> cursosDocente = cursoRepository.findByDocentesId(usuario.getId());
                idsDocente.addAll(cursosDocente.stream().map(Curso::getIdOferta).collect(Collectors.toSet()));
                
                // Formaciones
                List<Formacion> formacionesDocente = formacionRepository.findByDocentesId(usuario.getId());
                idsDocente.addAll(formacionesDocente.stream().map(Formacion::getIdOferta).collect(Collectors.toSet()));
            });
        }

        model.addAttribute("instituto", instituto);
        model.addAttribute("imagenesCarrusel", imagenesCarrusel);
        model.addAttribute("ofertas", ofertas);
        model.addAttribute("categorias", categorias);
        model.addAttribute("idsInscritos", idsInscritos);
        model.addAttribute("idsDocente", idsDocente);

        return "publico";
    }

    @GetMapping("/publico/oferta/{id}")
    public String verDetalleOferta(@PathVariable("id") Long id, Model model, Principal principal) {
        OfertaAcademica oferta = ofertaAcademicaRepository.findById(id).orElse(null);
        
        if (oferta == null || (oferta.getEstado() != EstadoOferta.ACTIVA && oferta.getEstado() != EstadoOferta.ENCURSO)) {
            return "redirect:/publico";
        }

        // Force intialization of Lazy collections to avoid LazyInitializationException in the view
        if (oferta instanceof Curso) {
            Curso curso = (Curso) oferta;
            if (curso.getDocentes() != null) curso.getDocentes().size();
        } else if (oferta instanceof Formacion) {
            Formacion formacion = (Formacion) oferta;
            if (formacion.getDocentes() != null) formacion.getDocentes().size();
        } else if (oferta instanceof Charla) {
            Charla charla = (Charla) oferta;
            if (charla.getDisertantes() != null) charla.getDisertantes().size();
        } else if (oferta instanceof Seminario) {
            Seminario seminario = (Seminario) oferta;
            if (seminario.getDisertantes() != null) seminario.getDisertantes().size();
        }
        
        // Verificar inscripciones y docencia del usuario logueado
        Set<Long> idsInscritos = new HashSet<>();
        Set<Long> idsDocente = new HashSet<>();

        if (principal != null) {
            String dni = principal.getName();
            usuarioRepository.findByDni(dni).ifPresent(usuario -> {
                // 1. Obtener IDs de ofertas donde está INSCRITO (Como Alumno)
                List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoId(usuario.getId());
                idsInscritos.addAll(inscripciones.stream()
                        .map(i -> i.getOferta().getIdOferta())
                        .collect(Collectors.toSet()));

                // 2. Obtener IDs de ofertas donde es DOCENTE
                // Cursos
                List<Curso> cursosDocente = cursoRepository.findByDocentesId(usuario.getId());
                idsDocente.addAll(cursosDocente.stream().map(Curso::getIdOferta).collect(Collectors.toSet()));
                
                // Formaciones
                List<Formacion> formacionesDocente = formacionRepository.findByDocentesId(usuario.getId());
                idsDocente.addAll(formacionesDocente.stream().map(Formacion::getIdOferta).collect(Collectors.toSet()));
            });
        }
        
        model.addAttribute("oferta", oferta);
        model.addAttribute("idsInscritos", idsInscritos);
        model.addAttribute("idsDocente", idsDocente);
        
        return "oferta-detalle";
    }

}