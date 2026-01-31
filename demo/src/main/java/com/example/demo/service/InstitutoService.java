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
     * Obtiene el primer instituto de la base de datos o crea uno por defecto
     */
    public Instituto obtenerInstituto() {
        return institutoRepository.findTopByOrderByIdInstitutoAsc()
                .orElse(crearInstitutoPorDefecto());
    }
    
    /**
     * Crea un instituto con valores por defecto
     */
    private Instituto crearInstitutoPorDefecto() {
        Instituto instituto = new Instituto();
        instituto.setNombreInstituto("Instituto Aurea");
        instituto.setDescripcion("Instituto de formación profesional y técnica");
        instituto.setMision("Brindar educación de calidad y formar profesionales competentes");
        instituto.setVision("Ser referentes en educación profesional a nivel nacional");
        instituto.setDireccion("Av. Corrientes 1234, CABA");
        instituto.setTelefono("+54 11 1234-5678");
        instituto.setEmail("contacto@institutoaurea.edu.ar");
        instituto.setFacebook("https://facebook.com/institutoaurea");
        instituto.setInstagram("https://instagram.com/institutoaurea");
        instituto.setX("https://x.com/institutoaurea");
        instituto.setMoneda("ARS");
        instituto.setCuentaBancaria("1234567890123456789012");
        instituto.setPoliticaPagos("Pago en cuotas disponible. Descuentos por pago completo.");
        instituto.setPermisoBajaAutomatica(true);
        instituto.setMinimoAlumnoBaja(5);
        instituto.setInactividadBaja(30);
        instituto.setDiasMoraBloqueoAula(null);
        instituto.setHabilitarIA(true);
        instituto.setReportesAutomaticos(false);
        instituto.setCertificacionesAvales("Contamos con el respaldo de instituciones reconocidas a nivel nacional e internacional");
        
        // Colores institucionales por defecto
        instituto.setColores(List.of("#1f2937", "#f8fafc", "#374151"));
        
        return institutoRepository.save(instituto);
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
        colores.put("colorPrimario", "#1f2937");
        colores.put("colorSecundario", "#f8fafc");
        colores.put("colorTexto", "#374151");
        return colores;
    }
}