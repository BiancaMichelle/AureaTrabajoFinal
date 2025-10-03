package com.example.demo.model;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Charla extends OfertaAcademica {
    private String lugar;
    private String enlace;
    private Integer duracionEstimada; // en minutos
    
    @ElementCollection
    private List<String> disertantes;
    
    private String publicoObjetivo;

    /**
     * Duración estimada formateada
     */
    public String getDuracionEstimadaTexto() {
        if (duracionEstimada == null) return "No definida";
        if (duracionEstimada < 60) return duracionEstimada + " min";
        return (duracionEstimada / 60) + "h " + (duracionEstimada % 60) + "min";
    }
    /**
     * Tipo de modalidad (presencial/online)
     */
    public String getTipoModalidad() {
        if (enlace != null && !enlace.trim().isEmpty()) {
            return "Online";
        }
        if (lugar != null && !lugar.trim().isEmpty()) {
            return "Presencial";
        }
        return "No definida";
    }
}
