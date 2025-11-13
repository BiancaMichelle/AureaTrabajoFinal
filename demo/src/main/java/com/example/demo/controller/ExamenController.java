package com.example.demo.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Examen;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.repository.ExamenRepository;

@Controller
@RequestMapping("/examen")
public class ExamenController {

    @Autowired
    private ExamenRepository examenRepository;

    /**
     * Muestra la vista del examen con todas las preguntas de todos los pools
     */
    @GetMapping("/realizar/{examenId}")
    public String realizarExamen(@PathVariable Long examenId, Principal principal, Model model) {
        try {
            Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen no encontrado"));
            
            // Obtener todas las preguntas de todos los pools
            List<Pregunta> todasLasPreguntas = examen.getPoolPreguntas().stream()
                .flatMap(pool -> pool.getPreguntas().stream())
                .collect(Collectors.toList());
            
            model.addAttribute("examen", examen);
            model.addAttribute("preguntas", todasLasPreguntas);
            model.addAttribute("tiempoTotal", examen.getTiempoRealizacion() != null ? examen.getTiempoRealizacion() : 60);
            
            return "examen";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * API REST para obtener los datos del examen en formato JSON
     */
    @GetMapping("/api/datos/{examenId}")
    public ResponseEntity<?> obtenerDatosExamen(@PathVariable Long examenId) {
        try {
            Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen no encontrado"));
            
            System.out.println("=== DEBUG EXAMEN ===");
            System.out.println("Examen ID: " + examenId);
            System.out.println("Pools: " + (examen.getPoolPreguntas() != null ? examen.getPoolPreguntas().size() : 0));
            
            // Construir el objeto de respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("titulo", examen.getTitulo());
            response.put("descripcion", examen.getDescripcion());
            response.put("tiempoRealizacion", examen.getTiempoRealizacion() != null ? examen.getTiempoRealizacion() : 60);
            
            // Construir preguntas desde todos los pools
            List<Map<String, Object>> preguntasDTO = examen.getPoolPreguntas().stream()
                .flatMap(pool -> {
                    System.out.println("Pool: " + pool.getNombre() + ", Preguntas: " + (pool.getPreguntas() != null ? pool.getPreguntas().size() : 0));
                    return pool.getPreguntas().stream().map(pregunta -> {
                        System.out.println("  Pregunta: " + pregunta.getEnunciado());
                        System.out.println("  Opciones: " + (pregunta.getOpciones() != null ? pregunta.getOpciones().size() : 0));
                        
                        Map<String, Object> preguntaMap = new HashMap<>();
                        preguntaMap.put("id", pregunta.getIdPregunta().toString());
                        preguntaMap.put("tipo", pregunta.getTipoPregunta().name());
                        preguntaMap.put("texto", pregunta.getEnunciado());
                        preguntaMap.put("puntaje", pregunta.getPuntaje());
                        
                        // Si tiene opciones, agregarlas
                        if (pregunta.getOpciones() != null && !pregunta.getOpciones().isEmpty()) {
                            List<Map<String, Object>> opciones = pregunta.getOpciones().stream()
                                .map(opcion -> {
                                    System.out.println("    Opcion: " + opcion.getDescripcion());
                                    Map<String, Object> opcionMap = new HashMap<>();
                                    opcionMap.put("id", opcion.getIdOpcion().toString());
                                    opcionMap.put("texto", opcion.getDescripcion());
                                    return opcionMap;
                                })
                                .collect(Collectors.toList());
                            preguntaMap.put("opciones", opciones);
                        } else {
                            System.out.println("    Sin opciones!");
                            preguntaMap.put("opciones", List.of());
                        }
                        
                        return preguntaMap;
                    });
                })
                .collect(Collectors.toList());
            
            response.put("preguntas", preguntasDTO);
            
            System.out.println("Total preguntas retornadas: " + preguntasDTO.size());
            System.out.println("===================");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al cargar el examen: " + e.getMessage()));
        }
    }
}
