package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.enums.TipoPregunta;
import com.example.demo.model.Examen;
import com.example.demo.service.ExamenService;
import com.example.demo.service.ExamenService.PoolDTO;
import com.example.demo.service.ExamenService.PreguntaDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/actividad")
public class ActividadController {

    @Autowired
    private ExamenService examenService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostMapping("/crearExamen")
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
    
    // Clases internas para recibir datos del frontend
    public static class PoolDTORequest {
        private Long id;
        private String nombre;
        private String descripcion;
        private Integer cantidadPreguntas;
        private List<PreguntaDTORequest> preguntas;
        
        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
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
