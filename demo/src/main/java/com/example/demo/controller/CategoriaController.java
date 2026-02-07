package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Categoria;
import com.example.demo.service.CategoriaService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<Categoria> getAllCategorias() {
        return categoriaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoria(@PathVariable Long id) {
        return categoriaService.findById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Categoria no encontrada");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            });
    }

    @PostMapping
    public ResponseEntity<?> createCategoria(@RequestBody Categoria categoria) {
        try {
            return ResponseEntity.ok(categoriaService.save(categoria));
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error en la respuesta del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        try {
            categoria.setIdCategoria(id);
            return ResponseEntity.ok(categoriaService.save(categoria));
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error en la respuesta del servidor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategoria(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            categoriaService.deleteById(id);
            response.put("success", true);
            response.put("message", "Categoria eliminada exitosamente");
            return ResponseEntity.ok(response);
            
        } catch (DataIntegrityViolationException e) {
            response.put("success", false);
            response.put("message", "No se puede eliminar la categoria porque esta siendo utilizada en uno o mas cursos/ofertas academicas. Primero debe remover la categoria de todas las ofertas.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la categoria: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}



