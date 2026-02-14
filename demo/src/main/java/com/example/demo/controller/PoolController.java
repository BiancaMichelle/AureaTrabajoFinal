package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Material;
import com.example.demo.model.Opcion;
import com.example.demo.model.Pool;
import com.example.demo.model.Pregunta;
import com.example.demo.repository.OfertaAcademicaRepository;
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
    
    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private com.example.demo.repository.MaterialRepository materialRepository;

    @Autowired
    private com.example.demo.service.IaPoolGeneratorService iaPoolGeneratorService;
    
    /**
     * Crear un pool independiente (sin examen asociado)
     */
    @PostMapping("/crear")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<String> crearPool(
            @RequestParam("nombrePool") String nombre,
            @RequestParam(value = "descripcionPool", required = false, defaultValue = "") String descripcion,
            @RequestParam("cantidadPreguntas") Integer cantidadPreguntas,
            @RequestParam("ofertaId") Long ofertaId,
            @RequestParam(value = "esIA", required = false, defaultValue = "false") Boolean esIA,
            @RequestParam(value = "iaParams", required = false) String iaParams) {
        try {
            Pool pool = new Pool();
            pool.setIdPool(UUID.randomUUID());
            pool.setNombre(nombre);
            pool.setDescripcion(descripcion);
            pool.setCantidadPreguntas(cantidadPreguntas);
            pool.setPreguntas(new ArrayList<>());
            // Asociar oferta para filtrar más tarde
            var oferta = ofertaAcademicaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada para ID: " + ofertaId));
            pool.setOferta(oferta);
            
            // Configurar IA si aplica
            if (Boolean.TRUE.equals(esIA)) {
                if (cantidadPreguntas != null && cantidadPreguntas > 8) {
                    return ResponseEntity.badRequest()
                        .header("Content-Type", "application/json")
                        .body("{\"success\": false, \"message\": \"El maximo de preguntas para un pool IA es 8.\"}");
                }
                pool.setGeneratedByIA(true);
                pool.setIaStatus(com.example.demo.enums.IaGenerationStatus.PENDING);
                pool.setIaRequest(iaParams);
            }
            
            Pool poolGuardado = poolRepository.save(pool);
            
            // Disparar proceso IA si aplica
            if (Boolean.TRUE.equals(esIA)) {
                iaPoolGeneratorService.generarPoolIAAsync(poolGuardado.getIdPool(), iaParams);
            }
            
            System.out.println("Pool creado: " + poolGuardado.getIdPool());
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body("{\"success\": true, \"message\": \"Pool creado exitosamente\", \"idPool\": \"" + poolGuardado.getIdPool() + "\", \"iaStatus\": \"" + pool.getIaStatus() + "\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"success\": false, \"message\": \"Error al crear el pool: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    /**
     * Consultar estado IA
     */
    @GetMapping("/estado/{poolId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> consultarEstadoIA(@PathVariable String poolId) {
        try {
            Pool pool = poolRepository.findById(UUID.fromString(poolId))
                .orElseThrow(() -> new RuntimeException("Pool no encontrado"));
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("status", pool.getIaStatus());
            response.put("errorMessage", pool.getIaErrorMessage());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
             return ResponseEntity.badRequest()
                 .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @GetMapping("/materiales/{ofertaId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerMateriales(@PathVariable Long ofertaId) {
        try {
            // Verificar que la oferta existe (opcional, pero buena práctica)
            if (!ofertaAcademicaRepository.existsById(ofertaId)) {
                return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "Oferta no encontrada"));
            }
            
            // Usar repositorio optimizado
            List<Material> materialesList = materialRepository.findByModulo_Curso_IdOferta(ofertaId);
            
            List<java.util.Map<String, Object>> materiales = new ArrayList<>();
            for (Material material : materialesList) {
                java.util.Map<String, Object> matMap = new java.util.HashMap<>();
                matMap.put("id", material.getIdActividad());
                matMap.put("titulo", material.getTitulo());
                // Obtener nombre del modulo de forma segura
                java.util.UUID moduloId = (material.getModulo() != null) ? material.getModulo().getIdModulo() : null;
                matMap.put("moduloId", moduloId != null ? moduloId.toString() : null);
                String moduloNombre = (material.getModulo() != null) ? material.getModulo().getNombre() : "Sin módulo";
                matMap.put("moduloNombre", moduloNombre);
                materiales.add(matMap);
            }
            
            return ResponseEntity.ok(materiales);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    // Antiguo endpoint renombrado o mantenido por compatibilidad si se usa directamente sin param IA
    // Se ha fusionado en el endpoint /crear principal para simplificar.
    
    @PostMapping("/editar/{poolId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<String> editarPool(
            @PathVariable String poolId,
            @RequestParam("nombrePool") String nombre,
            @RequestParam(value = "descripcionPool", required = false, defaultValue = "") String descripcion,
            @RequestParam("cantidadPreguntas") Integer cantidadPreguntas) {
        try {
            Pool pool = poolRepository.findById(UUID.fromString(poolId))
                .orElseThrow(() -> new RuntimeException("Pool no encontrado"));
                
            pool.setNombre(nombre);
            pool.setDescripcion(descripcion);
            pool.setCantidadPreguntas(cantidadPreguntas);
            
            poolRepository.save(pool);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body("{\"success\": true, \"message\": \"Pool actualizado exitosamente\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"success\": false, \"message\": \"Error al actualizar el pool: " + e.getMessage().replace("\"", "'") + "\"}");
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
     * Editar una pregunta existente
     */
    @PostMapping("/editarPregunta/{preguntaId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<String> editarPregunta(
            @PathVariable String preguntaId,
            @RequestParam("enunciado") String enunciado,
            @RequestParam("tipoPregunta") String tipoPregunta,
            @RequestParam("puntaje") Float puntaje,
            @RequestParam(value = "opcionesData", required = false, defaultValue = "[]") String opcionesData) {
        try {
            Pregunta pregunta = preguntaRepository.findById(UUID.fromString(preguntaId))
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));
            
            pregunta.setEnunciado(enunciado);
            pregunta.setTipoPregunta(TipoPregunta.valueOf(tipoPregunta));
            pregunta.setPuntaje(puntaje);
            
            // Actualizar opciones
            if (opcionesData != null && !opcionesData.isEmpty() && !opcionesData.equals("[]")) {
                List<OpcionDTO> opcionesList = objectMapper.readValue(opcionesData, new TypeReference<List<OpcionDTO>>(){});
                
                // Limpiar opciones actuales
                pregunta.getOpciones().clear();
                
                for (OpcionDTO opcionDTO : opcionesList) {
                    Opcion opcion = new Opcion();
                    opcion.setIdOpcion(UUID.randomUUID());
                    opcion.setDescripcion(opcionDTO.getDescripcion());
                    opcion.setEsCorrecta(opcionDTO.getEsCorrecta());
                    opcion.setPregunta(pregunta);
                    pregunta.getOpciones().add(opcion);
                }
            } else {
                 pregunta.getOpciones().clear();
            }
            
            preguntaRepository.save(pregunta);
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body("{\"success\": true, \"message\": \"Pregunta actualizada exitosamente\"}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"success\": false, \"message\": \"Error al actualizar pregunta: " + e.getMessage().replace("\"", "'") + "\"}");
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
            List<java.util.Map<String, Object>> dto = pools.stream().map(p -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("idPool", p.getIdPool());
                m.put("nombre", p.getNombre());
                m.put("descripcion", p.getDescripcion());
                m.put("cantidadPreguntas", p.getCantidadPreguntas());
                m.put("preguntasCount", p.getPreguntas() != null ? p.getPreguntas().size() : 0);
                return m;
            }).toList();
            return ResponseEntity.ok(dto);
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
            // Filtrar pools por la oferta indicada
            List<Pool> pools = poolRepository.findByOferta_IdOferta(ofertaId);
            System.out.println("=== LISTAR POOLS ===");
            System.out.println("Oferta ID solicitada: " + ofertaId);
            System.out.println("Total pools en BD: " + pools.size());
            for (Pool pool : pools) {
                System.out.println("  - Pool: " + pool.getNombre() + " (ID: " + pool.getIdPool() + ")");
                System.out.println("    Preguntas: " + (pool.getPreguntas() != null ? pool.getPreguntas().size() : 0));
            }
            System.out.println("====================");
            List<java.util.Map<String, Object>> dto = pools.stream().map(p -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("idPool", p.getIdPool());
                m.put("nombre", p.getNombre());
                m.put("descripcion", p.getDescripcion());
                m.put("cantidadPreguntas", p.getCantidadPreguntas());
                m.put("preguntasCount", p.getPreguntas() != null ? p.getPreguntas().size() : 0);
                return m;
            }).toList();
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Error al listar pools: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Obtener detalle de un pool con todas sus preguntas y opciones
     */
    @GetMapping("/detalle/{poolId}")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> obtenerDetallePool(@PathVariable String poolId) {
        try {
            Pool pool = poolRepository.findById(UUID.fromString(poolId))
                .orElseThrow(() -> new RuntimeException("Pool no encontrado"));
            
            List<java.util.Map<String, Object>> preguntasDTO = new ArrayList<>();
            
            if (pool.getPreguntas() != null) {
                for (Pregunta pregunta : pool.getPreguntas()) {
                    java.util.Map<String, Object> preguntaMap = new java.util.HashMap<>();
                    preguntaMap.put("idPregunta", pregunta.getIdPregunta());
                    preguntaMap.put("enunciado", pregunta.getEnunciado());
                    preguntaMap.put("tipoPregunta", pregunta.getTipoPregunta().toString());
                    preguntaMap.put("puntaje", pregunta.getPuntaje());
                    
                    // Agregar opciones si existen
                    List<java.util.Map<String, Object>> opcionesDTO = new ArrayList<>();
                    if (pregunta.getOpciones() != null) {
                        for (Opcion opcion : pregunta.getOpciones()) {
                            java.util.Map<String, Object> opcionMap = new java.util.HashMap<>();
                            opcionMap.put("idOpcion", opcion.getIdOpcion());
                            opcionMap.put("descripcion", opcion.getDescripcion());
                            opcionMap.put("esCorrecta", opcion.getEsCorrecta());
                            opcionesDTO.add(opcionMap);
                        }
                    }
                    preguntaMap.put("opciones", opcionesDTO);
                    preguntasDTO.add(preguntaMap);
                }
            }
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("pool", pool.getNombre());
            response.put("descripcion", pool.getDescripcion());
            response.put("cantidadPreguntas", pool.getCantidadPreguntas());
            response.put("preguntas", preguntasDTO);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Error al obtener detalle del pool: " + e.getMessage() + "\"}");
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
