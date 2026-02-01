package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.Modulo;
import com.example.demo.model.Recurso;
import com.example.demo.model.TipoRecurso;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.RecursoRepository;

@Controller
@RequestMapping("/recurso")
public class RecursoController {
    
    private final RecursoRepository recursoRepository;
    private final ModuloRepository moduloRepository;
    
    public RecursoController(RecursoRepository recursoRepository, ModuloRepository moduloRepository) {
        this.recursoRepository = recursoRepository;
        this.moduloRepository = moduloRepository;
    }
    
    @PostMapping("/crear")
    public ResponseEntity<?> crearRecurso(@RequestParam UUID moduloId,
                                          @RequestParam String titulo,
                                          @RequestParam(required = false) String descripcion,
                                          @RequestParam String tipo,
                                          @RequestParam(required = false) String url,
                                          @RequestParam(required = false) String contenido) {
        try {
            Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));
                
            Recurso recurso = new Recurso();
            recurso.setTitulo(titulo);
            recurso.setDescripcion(descripcion);
            recurso.setModulo(modulo);
            recurso.setFechaCreacion(LocalDateTime.now());
            recurso.setVisibilidad(true);
            try {
                recurso.setTipo(TipoRecurso.valueOf(tipo));
            } catch (IllegalArgumentException e) {
                 return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tipo de recurso inválido"));
            }
            recurso.setUrl(url);
            recurso.setContenidoTexto(contenido);
            
            recursoRepository.save(recurso);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Recurso creado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
}
