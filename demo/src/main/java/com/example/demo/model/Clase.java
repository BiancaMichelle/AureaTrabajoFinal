package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Clase {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id_clase", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID idClase;

    private String titulo;
    private String descripcion;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    // Campos para la videoconferencia
    private String roomName;

    @Column(length = 2048) // Aumentar longitud para soportar URLs con JWT
    private String meetingUrl;

    private Boolean asistenciaAutomatica;
    private Boolean transcripcionHabilitada;
    private Boolean preguntasAleatorias;
    private Integer cantidadPreguntas;
    
    // Configuración de resumen automático (CU-27)
    private Boolean generarResumenAutomatico;
    private Boolean publicarResumenAutomaticamente;
    
    // Permisos de alumnos durante la clase (CU-26)
    private Boolean permisoMicrofono;
    private Boolean permisoCamara;
    private Boolean permisoCompartirPantalla;
    private Boolean permisoChat;

    @ManyToOne
    @JoinColumn(name = "curso_id")
    private OfertaAcademica curso; // Puede ser Curso, Formación, etc.

    @ManyToOne
    @JoinColumn(name = "docente_dni")
    private Docente docente;

    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    // Método para generar automáticamente el roomName si no existe

    public void generateRoomName() {
        if (this.roomName == null && this.titulo != null) {
            String baseName = this.titulo.toLowerCase()
                    .replace(" ", "-")
                    .replaceAll("[^a-z0-9-]", "");
            this.roomName = "clase-" + baseName + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    public Boolean getTranscripcionHabilitada() {
        return transcripcionHabilitada;
    }

    public void setTranscripcionHabilitada(Boolean transcripcionHabilitada) {
        this.transcripcionHabilitada = transcripcionHabilitada;
    }
}