package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.enums.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // Obtener estadísticas básicas
        long totalOfertas = ofertaAcademicaRepository.count();
        long totalCategorias = categoriaRepository.count();
        
        model.addAttribute("totalOfertas", totalOfertas);
        model.addAttribute("totalCategorias", totalCategorias);
        
        return "admin/panelAdmin";
    }

    @GetMapping("/admin/gestion-ofertas")
    public String gestionOfertas(Model model) {
        List<Categoria> categorias = categoriaRepository.findAll();
        
        model.addAttribute("categorias", categorias);
        model.addAttribute("modalidades", Modalidad.values());
        model.addAttribute("estados", EstadoOferta.values());
        
        return "admin/gestionOfertas";
    }

    @PostMapping("/admin/ofertas/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearOferta(@RequestBody Map<String, Object> datos) {
        try {
            // Crear una oferta académica básica para demostrar funcionalidad
            OfertaAcademica oferta = new OfertaAcademica();
            oferta.setNombre((String) datos.get("nombre"));
            oferta.setDescripcion((String) datos.get("descripcion"));
            
            if (datos.get("cupos") != null) {
                oferta.setCupos(Integer.valueOf(datos.get("cupos").toString()));
            }
            
            if (datos.get("costo") != null) {
                oferta.setCostoInscripcion(Double.valueOf(datos.get("costo").toString()));
            }
            
            if (datos.get("fechaInicio") != null) {
                oferta.setFechaInicio(LocalDate.parse((String) datos.get("fechaInicio")));
            }
            
            if (datos.get("fechaFin") != null) {
                oferta.setFechaFin(LocalDate.parse((String) datos.get("fechaFin")));
            }
            
            // Configurar modalidad
            String modalidadStr = (String) datos.get("modalidad");
            if (modalidadStr != null) {
                oferta.setModalidad(Modalidad.valueOf(modalidadStr.toUpperCase()));
            }
            
            // Configurar estado por defecto
            oferta.setEstado(EstadoOferta.ACTIVA);
            oferta.setVisibilidad(true);
            
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta creada exitosamente");
            response.put("id", nuevaOferta.getIdOferta());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al crear oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/admin/ofertas/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarOferta(@RequestParam String tipoOferta,
                                 @RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) Integer cupos,
                                 @RequestParam(required = false) Double costo,
                                 @RequestParam(required = false) String fechaInicio,
                                 @RequestParam(required = false) String fechaFin,
                                 @RequestParam(required = false) String modalidad) {
        try {
            // Crear nueva oferta académica
            OfertaAcademica oferta = new OfertaAcademica();
            oferta.setNombre(nombre);
            oferta.setDescripcion(descripcion);
            
            if (cupos != null) {
                oferta.setCupos(cupos);
            }
            
            if (costo != null) {
                oferta.setCostoInscripcion(costo);
            }
            
            if (fechaInicio != null && !fechaInicio.isEmpty()) {
                oferta.setFechaInicio(LocalDate.parse(fechaInicio));
            }
            
            if (fechaFin != null && !fechaFin.isEmpty()) {
                oferta.setFechaFin(LocalDate.parse(fechaFin));
            }
            
            // Configurar modalidad
            if (modalidad != null && !modalidad.isEmpty()) {
                oferta.setModalidad(Modalidad.valueOf(modalidad.toUpperCase()));
            }
            
            // Configurar estado por defecto
            oferta.setEstado(EstadoOferta.ACTIVA);
            oferta.setVisibilidad(true);
            
            OfertaAcademica nuevaOferta = ofertaAcademicaRepository.save(oferta);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Oferta registrada exitosamente");
            response.put("id", nuevaOferta.getIdOferta());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al registrar oferta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/ofertas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertas() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertas);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/admin/ofertas/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarOfertasAjax() {
        try {
            List<OfertaAcademica> ofertas = ofertaAcademicaRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ofertas);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener ofertas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}