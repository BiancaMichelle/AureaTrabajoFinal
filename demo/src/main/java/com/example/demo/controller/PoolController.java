package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Opcion;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.repository.OpcionRepository;
import com.example.demo.repository.PoolRepository;
import com.example.demo.repository.PreguntaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/pool")
public class PoolController {

    @Autowired
    private PoolRepository poolRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private OpcionRepository opcionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Crear un pool independiente (sin examen asociado)
     */
    @PostMapping("/crear")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<String> crearPool(
            @RequestParam("nombrePool") String nombre,
            @RequestParam(value = "descripcionPool", required = false, defaultValue = "") String descripcion,
            @RequestParam("cantidadPreguntas") Integer cantidadPreguntas) {
        try {
            Pool pool = new Pool();
            pool.setIdPool(UUID.randomUUID());
            pool.setNombre(nombre);
            pool.setDescripcion(descripcion);
            pool.setCantidadPreguntas(cantidadPreguntas);
            pool.setPreguntas(new ArrayList<>());
            
            Pool poolGuardado = poolRepository.save(pool);
            
            System.out.println("Pool creado: " + poolGuardado.getIdPool());
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body("{\"success\": true, \"message\": \"Pool creado exitosamente\", \"idPool\": \"" + poolGuardado.getIdPool() + "\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"success\": false, \"message\": \"Error al crear el pool: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
    
    /**
     * Agregar pregunta a un pool existente
     */
    @PostMapping("/agregarPregunta/{poolId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<String> agregarPregunta(
            @PathVariable String poolId,
            @RequestParam("enunciado") String enunciado,
            @RequestParam("tipoPregunta") String tipoPregunta,
            @RequestParam("puntaje") Float puntaje,
            @RequestParam(value = "opcionesData", required = false, defaultValue = "[]") String opcionesData) {
        try {
            System.out.println("=== DEBUG POOL CONTROLLER ===");
            System.out.println("Pool ID: " + poolId);
            System.out.println("Enunciado: " + enunciado);
            System.out.println("Tipo: " + tipoPregunta);
            System.out.println("Puntaje: " + puntaje);
            System.out.println("Opciones Data (raw): " + opcionesData);
            
            Pool pool = poolRepository.findById(UUID.fromString(poolId))
                .orElseThrow(() -> new RuntimeException("Pool no encontrado"));
            
            // Crear la pregunta
            Pregunta pregunta = new Pregunta();
            pregunta.setIdPregunta(UUID.randomUUID());
            pregunta.setEnunciado(enunciado);
            pregunta.setTipoPregunta(TipoPregunta.valueOf(tipoPregunta));
            pregunta.setPuntaje(puntaje);
            pregunta.setPool(pool);
            
            // Guardar pregunta primero
            Pregunta preguntaGuardada = preguntaRepository.save(pregunta);
            System.out.println("Pregunta guardada con ID: " + preguntaGuardada.getIdPregunta());
            
            // Si tiene opciones, crearlas
            if (opcionesData != null && !opcionesData.isEmpty() && !opcionesData.equals("[]")) {
                List<OpcionDTO> opcionesList = objectMapper.readValue(opcionesData, new TypeReference<List<OpcionDTO>>(){});
                System.out.println("Opciones parseadas: " + opcionesList.size());
                
                List<Opcion> opciones = new ArrayList<>();
                for (OpcionDTO opcionDTO : opcionesList) {
                    System.out.println("  - Opcion: " + opcionDTO.getDescripcion() + " (correcta: " + opcionDTO.getEsCorrecta() + ")");
                    
                    Opcion opcion = new Opcion();
                    opcion.setIdOpcion(UUID.randomUUID());
                    opcion.setDescripcion(opcionDTO.getDescripcion());
                    opcion.setEsCorrecta(opcionDTO.getEsCorrecta());
                    opcion.setPregunta(preguntaGuardada);
                    
                    Opcion opcionGuardada = opcionRepository.save(opcion);
                    System.out.println("    Guardada con ID: " + opcionGuardada.getIdOpcion());
                    opciones.add(opcionGuardada);
                }
                
                preguntaGuardada.setOpciones(opciones);
                System.out.println("Total opciones guardadas: " + opciones.size());
            } else {
                System.out.println("NO HAY OPCIONES PARA GUARDAR");
            }
            
            System.out.println("=============================");
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body("{\"success\": true, \"message\": \"Pregunta agregada exitosamente\", \"idPregunta\": \"" + preguntaGuardada.getIdPregunta() + "\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"success\": false, \"message\": \"Error al agregar pregunta: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
    
    /**
     * Listar todos los pools disponibles
     */
    @GetMapping("/listar")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> listarPools() {
        try {
            List<Pool> pools = poolRepository.findAll();
            return ResponseEntity.ok(pools);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Error al listar pools: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Listar pools por oferta (navega la jerarquía desde exámenes)
     */
    @GetMapping("/listar/{ofertaId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> listarPoolsPorOferta(@PathVariable Long ofertaId) {
        try {
            // Por ahora retorna todos los pools
            // TODO: Filtrar por oferta navegando: Pool -> Examenes -> Modulo -> OfertaAcademica
            List<Pool> pools = poolRepository.findAll();
            return ResponseEntity.ok(pools);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Error al listar pools: " + e.getMessage() + "\"}");
        }
    }
    
    // DTO para recibir opciones desde el frontend
    public static class OpcionDTO {
        private String descripcion;
        private Boolean esCorrecta;
        
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        
        public Boolean getEsCorrecta() { return esCorrecta; }
        public void setEsCorrecta(Boolean esCorrecta) { this.esCorrecta = esCorrecta; }
    }
}
