package com.example.demo.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.enums.TipoEntrega;
import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Archivo;
import com.example.demo.model.Examen;
import com.example.demo.model.Material;
import com.example.demo.model.Modulo;
import com.example.demo.model.Tarea;
import com.example.demo.repository.ArchivoRepository;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.service.ExamenService;
import com.example.demo.service.TareaService;
import com.example.demo.service.ExamenService.PoolDTO;
import com.example.demo.service.ExamenService.PreguntaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ActividadController {

    @Autowired
    private ExamenService examenService;
    
    @Autowired
    private TareaService tareaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ModuloRepository moduloRepository;
    
    @Autowired
    private MaterialRepository materialRepository;
    
    @Autowired
    private ArchivoRepository archivoRepository;
    
    @PostMapping("/actividad/crearExamen")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> crearExamen(
            @RequestParam("moduloId") String moduloId,
            @RequestParam("titulo") String titulo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam("fechaApertura") String fechaApertura,
            @RequestParam("fechaCierre") String fechaCierre,
            @RequestParam(value = "tiempoRealizacion", required = false, defaultValue = "60") Integer tiempoRealizacion,
            @RequestParam(value = "cantidadIntentos", required = false, defaultValue = "1") Integer cantidadIntentos,
            @RequestParam(value = "calificacionAutomatica", required = false, defaultValue = "false") Boolean calificacionAutomatica,
            @RequestParam(value = "publicarNota", required = false, defaultValue = "false") Boolean publicarNota,
            @RequestParam(value = "visibilidad", required = false, defaultValue = "false") Boolean visibilidad,
            @RequestParam(value = "poolsData", required = false) String poolsData) {
        
        try {
            // Crear el examen
            Examen examen = new Examen();
            examen.setTitulo(titulo);
            examen.setDescripcion(descripcion);
            examen.setFechaApertura(LocalDateTime.parse(fechaApertura));
            examen.setFechaCierre(LocalDateTime.parse(fechaCierre));
            examen.setTiempoRealizacion(tiempoRealizacion);
            examen.setCantidadIntentos(cantidadIntentos);
            examen.setCalificacionAutomatica(calificacionAutomatica);
            examen.setPublicarNota(publicarNota);
            examen.setVisibilidad(visibilidad);
            
            // Parsear los pools desde JSON
            List<PoolDTORequest> poolsRequest = null;
            if (poolsData != null && !poolsData.isEmpty() && !poolsData.equals("[]")) {
                poolsRequest = objectMapper.readValue(poolsData, new TypeReference<List<PoolDTORequest>>(){});
            }
            
            // Convertir a PoolDTO del servicio
            List<PoolDTO> pools = null;
            if (poolsRequest != null && !poolsRequest.isEmpty()) {
                pools = poolsRequest.stream().map(this::convertirAPoolDTO).toList();
            }
            
            // Guardar el examen
            Examen examenGuardado = examenService.crearExamen(examen, UUID.fromString(moduloId), pools);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Examen creado exitosamente\", \"idExamen\": " + examenGuardado.getIdActividad() + "}");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"Error al crear el examen: " + e.getMessage() + "\"}");
        }
    }
    
    private PoolDTO convertirAPoolDTO(PoolDTORequest request) {
        PoolDTO poolDTO = new PoolDTO();
        poolDTO.setIdReal(request.getIdReal());
        poolDTO.setEsExistente(request.getEsExistente());
        poolDTO.setNombre(request.getNombre());
        poolDTO.setDescripcion(request.getDescripcion());
        poolDTO.setCantidadPreguntas(request.getCantidadPreguntas());
        
        if (request.getPreguntas() != null && !request.getPreguntas().isEmpty()) {
            List<PreguntaDTO> preguntasDTO = request.getPreguntas().stream()
                .map(this::convertirAPreguntaDTO)
                .toList();
            poolDTO.setPreguntas(preguntasDTO);
        }
        
        return poolDTO;
    }
    
    private PreguntaDTO convertirAPreguntaDTO(PreguntaDTORequest request) {
        PreguntaDTO preguntaDTO = new PreguntaDTO();
        preguntaDTO.setEnunciado(request.getEnunciado());
        preguntaDTO.setTipoPregunta(TipoPregunta.valueOf(request.getTipoPregunta()));
        preguntaDTO.setPuntaje(request.getPuntaje());
        return preguntaDTO;
    }

    /**
     * Endpoint para crear una nueva tarea siguiendo el flujo del CU-30.
     * 
     * Precondiciones verificadas:
     * - @PreAuthorize valida que el usuario sea DOCENTE o ADMIN (Precondición 3)
     * - TareaService valida que el módulo exista (Precondición 1)
     * - TareaService valida que el curso esté en estado ACTIVA (Precondición 2)
     */
    @PostMapping("/actividad/crearTarea")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<?> crearTarea(
            @RequestParam("moduloId") String moduloId,
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("limiteEntrega") String limiteEntrega,
            @RequestParam(value = "tipoEntrega", required = false) List<String> tiposEntrega,
            @RequestParam(value = "entregasTardias", required = false, defaultValue = "false") Boolean entregasTardias,
            @RequestParam(value = "modificaciones", required = false, defaultValue = "false") Boolean modificaciones,
            @RequestParam(value = "visibilidad", required = false, defaultValue = "true") Boolean visibilidad) {
        
        try {
            // Paso 3 del CU-30: El docente completa los datos
            Tarea tarea = new Tarea();
            tarea.setTitulo(titulo);
            tarea.setDescripcion(descripcion);
            tarea.setLimiteEntrega(LocalDateTime.parse(limiteEntrega));
            
            // Paso 2: Convertir tipos de entrega de String a enum
            if (tiposEntrega != null && !tiposEntrega.isEmpty()) {
                List<TipoEntrega> tiposEntregaEnum = new ArrayList<>();
                for (String tipo : tiposEntrega) {
                    try {
                        tiposEntregaEnum.add(TipoEntrega.valueOf(tipo));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Tipo de entrega inválido: " + tipo));
                    }
                }
                tarea.setTipoEntrega(tiposEntregaEnum);
            }
            
            // Pasos 4-5: Configuración de entregas
            tarea.setEntregasTardias(entregasTardias);
            tarea.setModificaciones(modificaciones);
            
            // Pasos 6-7: Configuración de visibilidad
            tarea.setVisibilidad(visibilidad);
            
            // Pasos 8-10: Validar datos y guardar (delegado al servicio)
            Tarea tareaGuardada = tareaService.crearTarea(tarea, UUID.fromString(moduloId));
            
            // Paso 10: Confirmar la carga exitosa
            return ResponseEntity.ok().body(Map.of(
                "success", true, 
                "message", "Tarea creada exitosamente" + (visibilidad ? " y publicada para los alumnos" : " (oculta para los alumnos)"),
                "idTarea", tareaGuardada.getIdActividad()
            ));
            
        } catch (IllegalArgumentException e) {
            // Paso 8 (alternativo): Error en formato de datos
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Error en el formato de los datos: " + e.getMessage()));
        } catch (RuntimeException e) {
            // Paso 8 (alternativo): Validación fallida o precondiciones no cumplidas
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al crear la tarea: " + e.getMessage()));
        }
    }

    @DeleteMapping("/actividad/{actividadId}/eliminar")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> eliminarActividad(@PathVariable Long actividadId, Authentication authentication) {
        try {
            examenService.eliminarActividad(actividadId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/actividad/modulo/{moduloId}/material")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearMaterialEnModulo(
            @PathVariable String moduloId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> files) {
        try {
            UUID moduloUUID = Objects.requireNonNull(UUID.fromString(moduloId), "El id del módulo es requerido");
            Modulo modulo = moduloRepository.findById(moduloUUID)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

            Material material = new Material();
            material.setTitulo(titulo);
            material.setDescripcion(descripcion);
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(true);
            material.setModulo(modulo);
            material.setCarpeta(null); // Sin carpeta, directamente en el módulo

            // Si hay archivos, guardarlos
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        Archivo archivo = new Archivo();
                        archivo.setNombre(file.getOriginalFilename());
                        archivo.setTipoMime(file.getContentType());
                        archivo.setTamano(file.getSize());
                        archivo.setContenido(file.getBytes());
                        archivo.setFechaSubida(LocalDateTime.now());
                        archivo.setMaterial(material);
                        archivo.setCarpeta(null); // Sin carpeta
                        
                        material.getArchivos().add(archivo);
                    }
                }
            }

            materialRepository.save(material);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Material creado exitosamente");
            response.put("materialId", material.getIdActividad());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al procesar los archivos: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al crear material: " + e.getMessage()));
        }
    }
    
    @GetMapping("/actividad/material/{materialId}/archivos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerArchivosMaterial(@PathVariable Long materialId) {
        List<Archivo> archivos = archivoRepository.findByMaterialIdActividad(materialId);
        
        List<Map<String, Object>> archivosInfo = archivos.stream().map(archivo -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", archivo.getIdArchivo());
            info.put("nombre", archivo.getNombre());
            info.put("tipoMime", archivo.getTipoMime());
            info.put("tamano", archivo.getTamano());
            info.put("fechaSubida", archivo.getFechaSubida());
            return info;
        }).toList();
        
        return ResponseEntity.ok(archivosInfo);
    }
    
    @GetMapping("/archivo/{archivoId}/descargar")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long archivoId, @RequestParam(required = false, defaultValue = "false") boolean download) {
        try {
            Archivo archivo = archivoRepository.findById(archivoId)
                    .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(archivo.getTipoMime()));
            
            org.springframework.http.ContentDisposition disposition = org.springframework.http.ContentDisposition
                    .builder(download ? "attachment" : "inline")
                    .filename(archivo.getNombre(), java.nio.charset.StandardCharsets.UTF_8)
                    .build();
            
            headers.setContentDisposition(disposition);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(archivo.getContenido());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    // Clases internas para recibir datos del frontend
    public static class PoolDTORequest {
        private Long id; // id temporal del frontend
        private String idReal; // UUID real del pool existente
        private Boolean esExistente;
        private String nombre;
        private String descripcion;
        private Integer cantidadPreguntas;
        private List<PreguntaDTORequest> preguntas;
        
        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getIdReal() { return idReal; }
        public void setIdReal(String idReal) { this.idReal = idReal; }
        public Boolean getEsExistente() { return esExistente; }
        public void setEsExistente(Boolean esExistente) { this.esExistente = esExistente; }
        
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        
        public Integer getCantidadPreguntas() { return cantidadPreguntas; }
        public void setCantidadPreguntas(Integer cantidadPreguntas) { this.cantidadPreguntas = cantidadPreguntas; }
        
        public List<PreguntaDTORequest> getPreguntas() { return preguntas; }
        public void setPreguntas(List<PreguntaDTORequest> preguntas) { this.preguntas = preguntas; }
    }
    
    public static class PreguntaDTORequest {
        private String enunciado;
        private String tipoPregunta;
        private Float puntaje;
        
        // Getters y Setters
        public String getEnunciado() { return enunciado; }
        public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
        
        public String getTipoPregunta() { return tipoPregunta; }
        public void setTipoPregunta(String tipoPregunta) { this.tipoPregunta = tipoPregunta; }
        
        public Float getPuntaje() { return puntaje; }
        public void setPuntaje(Float puntaje) { this.puntaje = puntaje; }
    }
}
