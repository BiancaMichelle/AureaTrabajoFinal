package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Categoria;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.CarruselImagenRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.InstitutoService;

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
    
    @GetMapping({"/", "/publico"})
    public String publico(Model model) {
        // Obtener datos del instituto
        Instituto instituto = institutoService.obtenerInstituto();
        
        // Obtener imágenes del carrusel
        List<CarruselImagen> imagenesCarrusel = carruselImagenRepository.findByInstitutoAndActivaTrueOrderByOrden(instituto);
        
        // Obtener ofertas académicas activas
        List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();

        // Obtener categorías
        List<Categoria> categorias = categoriaRepository.findAll();
        
        model.addAttribute("instituto", instituto);
        model.addAttribute("imagenesCarrusel", imagenesCarrusel);
        model.addAttribute("ofertas", ofertas);
        model.addAttribute("categorias", categorias);

        return "publico";
    }

}