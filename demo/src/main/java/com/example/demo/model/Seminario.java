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
public class Seminario extends OfertaAcademica {
    private String lugar;
    private String enlace;
    private String publicoObjetivo;

    private Integer duracionMinutos; // en minutos

    @ElementCollection
    private List<String> disertantes;

    /**
     * Duración formateada
     */
    public String getDuracionTexto() {
        if (duracionMinutos == null) return "No definida";
        if (duracionMinutos < 60) return duracionMinutos + " min";
        return (duracionMinutos / 60) + "h " + (duracionMinutos % 60) + "min";
    }

}
