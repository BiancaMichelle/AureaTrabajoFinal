package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Categoria {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCategoria;
    private String nombre;
    private String descripcion;

    

    public Categoria(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
    /**
     * Devuelve el nombre de la categoría en formato capitalizado
     * Útil para mostrar en interfaces de usuario
     */
    public String getNombreCapitalizado() {
        if (nombre == null || nombre.isEmpty()) {
            return "";
        }
        return nombre.substring(0, 1).toUpperCase() + 
               nombre.substring(1).toLowerCase();
    }
    /**
     * Verifica si la categoría es válida (tiene nombre)
     * Útil para validaciones antes de guardar
     */
    public boolean esValida() {
        return nombre != null && !nombre.trim().isEmpty();
    }
    /**
     * Genera un slug/código amigable para URLs
     * Ejemplo: "Programación Web" -> "programacion-web"
     */
    public String getSlug() {
        if (nombre == null) return "";
        return nombre.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-")
                    .trim();
    }
    /**
     * Devuelve una descripción corta (primeras 50 caracteres)
     * Útil para listados y tarjetas
     */
    public String getDescripcionCorta() {
        if (descripcion == null || descripcion.length() <= 50) {
            return descripcion;
        }
        return descripcion.substring(0, 47) + "...";
    }
    /**
     * Verifica si contiene una palabra clave (búsqueda)
     * Útil para filtros y búsquedas
     */
    public boolean contienePalabra(String palabra) {
        if (palabra == null || palabra.trim().isEmpty()) {
            return false;
        }
        String palabraLower = palabra.toLowerCase();
        return (nombre != null && nombre.toLowerCase().contains(palabraLower)) ||
               (descripcion != null && descripcion.toLowerCase().contains(palabraLower));
    }
    /**
     * Actualiza los datos de la categoría
     * Útil para operaciones de actualización
     */
    public void modificarar(String nuevoNombre, String nuevaDescripcion) {
        if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
            this.nombre = nuevoNombre.trim();
        }
        if (nuevaDescripcion != null) {
            this.descripcion = nuevaDescripcion.trim();
        }
    }
    
        // ============= OVERRIDE METHODS =============
    
    @Override
    public String toString() {
        return "Categoria{" +
                "id=" + idCategoria +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + getDescripcionCorta() + '\'' +
                '}';
    }
    
    // ============= MÉTODOS PARA UI/FRONTEND =============
    
    /**
     * Devuelve el icono CSS basado en el nombre de la categoría
     * Útil para mostrar iconos dinámicos en el frontend
     */
    public String getIconoCSS() {
        if (nombre == null) return "fas fa-folder";
        
        String nombreLower = nombre.toLowerCase();
        
        if (nombreLower.contains("programacion") || nombreLower.contains("desarrollo")) {
            return "fas fa-code";
        } else if (nombreLower.contains("diseño") || nombreLower.contains("arte")) {
            return "fas fa-palette";
        } else if (nombreLower.contains("marketing") || nombreLower.contains("ventas")) {
            return "fas fa-chart-line";
        } else if (nombreLower.contains("administracion") || nombreLower.contains("gestion")) {
            return "fas fa-briefcase";
        } else if (nombreLower.contains("idioma")) {
            return "fas fa-language";
        } else if (nombreLower.contains("oficio") || nombreLower.contains("tecnico")) {
            return "fas fa-tools";
        } else if (nombreLower.contains("salud") || nombreLower.contains("medicina")) {
            return "fas fa-heartbeat";
        } else if (nombreLower.contains("tecnologia") || nombreLower.contains("informatica")) {
            return "fas fa-microchip";
        } else if (nombreLower.contains("finanza") || nombreLower.contains("contabilidad")) {
            return "fas fa-dollar-sign";
        } else if (nombreLower.contains("educacion") || nombreLower.contains("pedagogia")) {
            return "fas fa-graduation-cap";
        } else if (nombreLower.contains("gastronomia") || nombreLower.contains("cocina")) {
            return "fas fa-utensils";
        } else {
            return "fas fa-folder";
        }
    }
    
    /**
     * Devuelve la clase CSS de color basada en la categoría
     * Útil para chips y badges de colores
     */
    public String getColorCSS() {
        if (nombre == null) return "category-default";
        
        String nombreLower = nombre.toLowerCase();
        
        if (nombreLower.contains("programacion")) return "category-blue";
        else if (nombreLower.contains("diseño")) return "category-purple";
        else if (nombreLower.contains("marketing")) return "category-cyan";
        else if (nombreLower.contains("administracion")) return "category-green";
        else if (nombreLower.contains("idioma")) return "category-orange";
        else if (nombreLower.contains("oficio")) return "category-red";
        else if (nombreLower.contains("salud")) return "category-pink";
        else if (nombreLower.contains("tecnologia")) return "category-gray";
        else if (nombreLower.contains("finanza")) return "category-teal";
        else if (nombreLower.contains("educacion")) return "category-indigo";
        else if (nombreLower.contains("gastronomia")) return "category-brown";
        else return "category-default";
    }
}
