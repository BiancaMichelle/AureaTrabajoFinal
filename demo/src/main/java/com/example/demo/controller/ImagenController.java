package com.example.demo.controller;

import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Instituto;
import com.example.demo.service.ImagenService;
import com.example.demo.service.InstitutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ImagenController {

    @Autowired
    private ImagenService imagenService;
    
    @Autowired
    private InstitutoService institutoService;

    // Endpoint principal para servir las imágenes del carrusel
    @GetMapping("/api/carrusel/imagen/{id}")
    public ResponseEntity<byte[]> obtenerImagenCarrusel(@PathVariable Long id) {
        Optional<CarruselImagen> imagen = imagenService.obtenerImagenCarrusel(id);
        
        if (imagen.isPresent()) {
            CarruselImagen img = imagen.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(img.getTipoMime()));
            headers.setContentLength(img.getTamaño());
            headers.setCacheControl("max-age=3600"); // Cache por 1 hora
            
            return new ResponseEntity<>(img.getDatos(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint alternativo para compatibilidad
    @GetMapping("/imagen/carrusel/{id}")
    public ResponseEntity<byte[]> obtenerImagenCarruselAlt(@PathVariable Long id) {
        return obtenerImagenCarrusel(id);
    }

    // API REST para subir múltiples imágenes al carrusel
    @PostMapping("/api/carrusel/subir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirImagenesCarrusel(
            @RequestParam("imagenes") MultipartFile[] imagenes) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validaciones
            if (imagenes == null || imagenes.length == 0) {
                response.put("success", false);
                response.put("message", "No se han seleccionado imágenes");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar cada imagen
            for (MultipartFile imagen : imagenes) {
                if (!imagen.isEmpty()) {
                    // Validar tipo de archivo
                    String contentType = imagen.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        response.put("success", false);
                        response.put("message", "Solo se permiten archivos de imagen");
                        return ResponseEntity.badRequest().body(response);
                    }
                    
                    // Validar tamaño (5MB máximo)
                    if (imagen.getSize() > 5 * 1024 * 1024) {
                        response.put("success", false);
                        response.put("message", "Las imágenes deben ser menores a 5MB");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            }

            Instituto instituto = institutoService.obtenerInstituto();
            List<CarruselImagen> imagenesGuardadas = imagenService.guardarMultiplesImagenesCarrusel(imagenes, instituto);
            
            response.put("success", true);
            response.put("message", "Imágenes subidas exitosamente: " + imagenes.length);
            response.put("cantidad", imagenes.length);
            response.put("imagenes", imagenesGuardadas.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al procesar las imágenes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API REST para eliminar imagen del carrusel
    @DeleteMapping("/api/carrusel/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarImagenCarrusel(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<CarruselImagen> imagen = imagenService.obtenerImagenCarrusel(id);
            if (!imagen.isPresent()) {
                response.put("success", false);
                response.put("message", "Imagen no encontrada");
                return ResponseEntity.badRequest().body(response);
            }
            
            imagenService.eliminarImagenCarrusel(id);
            
            response.put("success", true);
            response.put("message", "Imagen eliminada exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la imagen: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API REST para actualizar orden de imagen
    @PutMapping("/api/carrusel/{id}/orden")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarOrdenImagen(
            @PathVariable Long id, 
            @RequestParam Integer nuevoOrden) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CarruselImagen imagen = imagenService.actualizarOrdenImagen(id, nuevoOrden);
            if (imagen == null) {
                response.put("success", false);
                response.put("message", "Imagen no encontrada");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("message", "Orden actualizado exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar el orden: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API REST para actualizar texto alternativo
    @PutMapping("/api/carrusel/{id}/alt-text")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarAltText(
            @PathVariable Long id, 
            @RequestParam String altText) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            CarruselImagen imagen = imagenService.actualizarAltText(id, altText);
            if (imagen == null) {
                response.put("success", false);
                response.put("message", "Imagen no encontrada");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("message", "Texto alternativo actualizado exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al actualizar el texto alternativo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API REST para obtener todas las imágenes del carrusel
    @GetMapping("/api/carrusel/imagenes")
    @ResponseBody
    public ResponseEntity<List<CarruselImagen>> obtenerImagenesCarrusel() {
        try {
            Instituto instituto = institutoService.obtenerInstituto();
            List<CarruselImagen> imagenes = imagenService.obtenerImagenesCarruselPorInstituto(instituto);
            return ResponseEntity.ok(imagenes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
