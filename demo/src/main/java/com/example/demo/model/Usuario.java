package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Pattern;

import com.example.demo.enums.TipoGenero;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
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

  @NotBlank(message = "El DNI es obligatorio")
  @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
  @Column(nullable = false, unique = true)
  private String dni;

  @NotBlank(message = "El nombre es obligatorio")
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras y espacios")
  private String nombre;

  @NotBlank(message = "El apellido es obligatorio")
  @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido solo puede contener letras y espacios")
  private String apellido;

  @Column(name = "foto")
    private String foto;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

  @NotNull(message = "La fecha de nacimiento es obligatoria")
  @Past(message = "La fecha de nacimiento debe ser en el pasado")
  private LocalDate fechaNacimiento;

  @NotNull(message = "El género es obligatorio")
  @Enumerated(EnumType.STRING)
  private TipoGenero genero;

  @NotBlank(message = "El correo es obligatorio")
  @Email(message = "El formato del correo electrónico no es válido")
  @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "El correo debe tener un dominio válido")
  private String correo;

  @NotBlank(message = "El teléfono es obligatorio")
  @Pattern(regexp = "^[\\d\\s\\-\\(\\)\\+]{10,}$", message = "El teléfono debe tener al menos 10 dígitos")
  private String numTelefono;

  @NotBlank(message = "La contraseña es obligatoria")
  @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", message = "La contraseña debe contener al menos una mayúscula y una minúscula")
  private String contraseña;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pais_codigo")
  private Pais pais;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provincia_codigo")
  private Provincia provincia;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ciudad_id")
  private Ciudad ciudad;

  private boolean estado = true;

  private boolean estadoCuenta;

    public String getEstado() {
        return estado ? "ACTIVO" : "INACTIVO";
    }

    public boolean isEstado() {
        return estado;
    }

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "usuarios_roles",
      joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id")
  )
  private Set<Rol> roles = new HashSet<>();
  
  // Relación con pagos según el diagrama
  @OneToMany(mappedBy = "usuario")
  private List<Pago> pagos;

  // Método de validación personalizado para la edad
  @AssertTrue(message = "Debes tener al menos 16 años")
  public boolean isMayorDe16() {
      if (fechaNacimiento == null) {
          return false;
      }
      return Period.between(fechaNacimiento, LocalDate.now()).getYears() >= 16;
  }

  @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}