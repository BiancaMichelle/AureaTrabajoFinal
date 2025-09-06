package com.example.demo.model;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
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
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo_usuario")
public class Usuario {
  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, unique = true)
  private String dni;

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
    name = "usuarios_roles",
    joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"), // UUID
    inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id") // BIGINT
  )
  private Set<Rol> roles = new HashSet<>();
}
