package com.example.demo.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Usuarios")
public class Usuario {
    @Id
    private String dni;
    @Column(unique = true, nullable = false)
    private String nombre;
    private String apellido;
    private Date fechaNacimiento;
    private String genero;
    private String correo;
    private String numTelefono;
    private String contraseña;
    @ManyToOne
    @JoinColumn(name = "pais_codigo")
    private Pais pais;
    
    @ManyToOne
    @JoinColumn(name = "provincia_codigo")
    private Provincia provincia;
    
    @ManyToOne
    @JoinColumn(name = "ciudad_id")
    private Ciudad ciudad;
    
    private String domicilio;
    private boolean estado = true;
    private boolean estadoCuenta;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Rol> roles = new HashSet<>();
}
