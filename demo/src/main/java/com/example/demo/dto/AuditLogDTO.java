package com.example.demo.dto;

import java.sql.Date;
import java.sql.Time;

/**
 * DTO para transferir información de auditoría sin exponer entidades completas
 */
public class AuditLogDTO {
    
    private Long id;
    private Date fecha;
    private Time hora;
    private String usuarioNombre;
    private String usuarioDni;
    private String rolNombre;
    private String accion;
    private String afecta;
    private String detalles;
    private String ip;
    private boolean exito;
    
    // Constructores
    public AuditLogDTO() {}
    
    public AuditLogDTO(Long id, Date fecha, Time hora, String usuarioNombre, 
                      String usuarioDni, String rolNombre, String accion, 
                      String afecta, String detalles, String ip, boolean exito) {
        this.id = id;
        this.fecha = fecha;
        this.hora = hora;
        this.usuarioNombre = usuarioNombre;
        this.usuarioDni = usuarioDni;
        this.rolNombre = rolNombre;
        this.accion = accion;
        this.afecta = afecta;
        this.detalles = detalles;
        this.ip = ip;
        this.exito = exito;
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
    
    public Time getHora() { return hora; }
    public void setHora(Time hora) { this.hora = hora; }
    
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    
    public String getUsuarioDni() { return usuarioDni; }
    public void setUsuarioDni(String usuarioDni) { this.usuarioDni = usuarioDni; }
    
    public String getRolNombre() { return rolNombre; }
    public void setRolNombre(String rolNombre) { this.rolNombre = rolNombre; }
    
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    
    public String getAfecta() { return afecta; }
    public void setAfecta(String afecta) { this.afecta = afecta; }
    
    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }
}