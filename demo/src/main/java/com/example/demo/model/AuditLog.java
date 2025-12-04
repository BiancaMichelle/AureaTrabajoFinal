package com.example.demo.model;
import java.sql.Date;
import java.sql.Time;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "fecha")
    private Date fecha;
    
    @Column(name = "hora")
    private Time hora;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol;
    
    @Column(name = "accion", length = 100)
    private String accion;
    
    @Column(name = "afecta", length = 100)
    private String afecta;
    
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;
    
    @Column(name = "ip", length = 45)
    private String ip;
    
    @Column(name = "exito")
    private boolean exito;

    

}
