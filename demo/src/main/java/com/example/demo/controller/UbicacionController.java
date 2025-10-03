package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Ciudad;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.service.LocacionAPIService;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final LocacionAPIService locacionApiService;

    public UbicacionController(LocacionAPIService locacionApiService) {
        this.locacionApiService = locacionApiService;
    }

    @GetMapping("/paises")
    public ResponseEntity<List<Pais>> obtenerTodosPaises() {
        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            return ResponseEntity.ok(paises);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provincias/{paisCode}")
    public ResponseEntity<List<Provincia>> obtenerProvincias(@PathVariable String paisCode) {
        try {
            System.out.println("🌍 Solicitando provincias para país: " + paisCode);
            List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(paisCode);
            
            System.out.println("✅ Provincias encontradas: " + provincias.size());
            
            // Log de las primeras 3 provincias para debug
            if (!provincias.isEmpty()) {
                System.out.println("📋 Primeras provincias:");
                provincias.stream().limit(3).forEach(p -> 
                    System.out.println("   - " + p.getNombre() + " (Código: " + p.getCodigo() + ", ID: " + p.getId() + ")")
                );
            }
            
            return ResponseEntity.ok(provincias);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo provincias para " + paisCode + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ciudades/{paisCode}/{provinciaCode}")
    public ResponseEntity<List<Ciudad>> obtenerCiudades(@PathVariable String paisCode, 
                                                       @PathVariable String provinciaCode) {
        try {
            List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCode, provinciaCode);
            return ResponseEntity.ok(ciudades);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}