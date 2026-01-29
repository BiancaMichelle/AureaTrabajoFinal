package com.example.demo.controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AsistenciaEnVivoService;

@RestController
@RequestMapping("/api/clase")
public class AsistenciaEnVivoController {

    private final AsistenciaEnVivoService asistenciaEnVivoService;

    public AsistenciaEnVivoController(AsistenciaEnVivoService asistenciaEnVivoService) {
        this.asistenciaEnVivoService = asistenciaEnVivoService;
    }

    @GetMapping("/{claseId}/verificar-pregunta")
    public ResponseEntity<?> verificarPregunta(@PathVariable UUID claseId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        
        try {
            Map<String, Object> pregunta = asistenciaEnVivoService.verificarPreguntaActiva(claseId, principal.getName());
            
            if (pregunta != null) {
                return ResponseEntity.ok(pregunta);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/{claseId}/responder-pregunta")
    public ResponseEntity<?> responderPregunta(@PathVariable UUID claseId, 
                                             @RequestBody Map<String, Object> payload, 
                                             Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        try {
            int ronda = Integer.parseInt(payload.get("ronda").toString());
            String respuesta = payload.get("respuesta").toString();
            
            boolean registrado = asistenciaEnVivoService.registrarRespuesta(claseId, principal.getName(), ronda, respuesta);
            
            return ResponseEntity.ok(Map.of("success", registrado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
