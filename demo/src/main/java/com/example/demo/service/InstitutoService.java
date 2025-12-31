package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Instituto;
import com.example.demo.repository.InstitutoRepository;

@Service
public class InstitutoService {

    @Autowired
    private InstitutoRepository institutoRepository;
    
    /**
     * Obtiene el primer instituto de la base de datos.
     * Se asume que DemoApplication garantiza la existencia de al menos un registro.
     */
    public Instituto obtenerInstituto() {
        return institutoRepository.findTopByOrderByIdInstitutoAsc()
                .orElseThrow(() -> new RuntimeException("Error crítico: No se encontró la configuración del instituto."));
    }
    
    /**
     * Guarda los cambios del instituto
     */
    public Instituto guardarInstituto(Instituto instituto) {
        return institutoRepository.save(instituto);
    }
    
    /**
     * Obtiene los colores institucionales (para compatibilidad)
     */
    public Map<String, String> obtenerColoresInstitucionales() {
        Map<String, String> colores = new HashMap<>();
        // Valores por defecto
        colores.put("colorPrimario", "#E5383B");
        colores.put("colorSecundario", "#0D1B2A");
        colores.put("colorTexto", "#374151");
        return colores;
    }
}