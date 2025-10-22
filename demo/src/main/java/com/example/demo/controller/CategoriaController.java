package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Categoria;
import com.example.demo.service.CategoriaService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    
    @Autowired
    private CategoriaService categoriaService;
    
    /**
     * Obtener todas las categorías
     * GET /api/categorias
     */
    @GetMapping
    public ResponseEntity<List<Categoria>> obtenerTodasLasCategorias() {
        try {
            List<Categoria> categorias = categoriaService.obtenerTodasLasCategorias();
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Obtener una categoría por ID
     * GET /api/categorias/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerCategoriaPorId(@PathVariable Long id) {
        try {
            Optional<Categoria> categoria = categoriaService.obtenerCategoriaPorId(id);
            if (categoria.isPresent()) {
                return ResponseEntity.ok(categoria.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Crear una nueva categoría
     * POST /api/categorias
     */
    @PostMapping
    public ResponseEntity<Object> crearCategoria(@RequestBody CategoriaRequest request) {
        try {
            // Validar request
            if (request == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Los datos de la categoría son requeridos"));
            }
            
            // Crear la categoría usando el servicio
            Categoria nuevaCategoria = categoriaService.crearCategoria(
                request.getNombre(), 
                request.getDescripcion()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
            
        } catch (IllegalArgumentException e) {
            // Error de validación de negocio
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            // Error interno del servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Actualizar una categoría existente
     * PUT /api/categorias/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarCategoria(@PathVariable Long id, @RequestBody CategoriaRequest request) {
        try {
            // Validar request
            if (request == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Los datos de la categoría son requeridos"));
            }
            
            // Actualizar usando el servicio
            Categoria categoriaActualizada = categoriaService.actualizarCategoria(
                id,
                request.getNombre(),
                request.getDescripcion()
            );
            
            return ResponseEntity.ok(categoriaActualizada);
            
        } catch (IllegalArgumentException e) {
            // Error de validación o categoría no encontrada
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            // Error interno del servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Eliminar una categoría
     * DELETE /api/categorias/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaService.eliminarCategoria(id);
            return ResponseEntity.ok()
                .body(new SuccessResponse("Categoría eliminada exitosamente"));
                
        } catch (IllegalArgumentException e) {
            // Categoría no encontrada
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
                
        } catch (Exception e) {
            // Error interno del servidor
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    /**
     * Buscar categorías por nombre
     * GET /api/categorias/buscar?nombre={nombre}
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<Categoria>> buscarCategorias(@RequestParam(required = false) String nombre) {
        try {
            List<Categoria> categorias = categoriaService.buscarCategoriasPorNombre(nombre);
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Verificar si existe una categoría con el nombre dado
     * GET /api/categorias/existe?nombre={nombre}
     */
    @GetMapping("/existe")
    public ResponseEntity<Boolean> existeCategoria(@RequestParam String nombre) {
        try {
            boolean existe = categoriaService.existeCategoriaPorNombre(nombre);
            return ResponseEntity.ok(existe);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // =============== CLASES AUXILIARES ===============
    
    /**
     * Clase para recibir datos de request
     */
    public static class CategoriaRequest {
        private String nombre;
        private String descripcion;
        
        // Constructores
        public CategoriaRequest() {}
        
        public CategoriaRequest(String nombre, String descripcion) {
            this.nombre = nombre;
            this.descripcion = descripcion;
        }
        
        // Getters y Setters
        public String getNombre() {
            return nombre;
        }
        
        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }
    }
    
    /**
     * Clase para respuestas de error
     */
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Clase para respuestas de éxito
     */
    public static class SuccessResponse {
        private String message;
        private long timestamp;
        
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}