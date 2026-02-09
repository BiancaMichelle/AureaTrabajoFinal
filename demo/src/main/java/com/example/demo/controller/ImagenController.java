package com.example.demo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.CarruselImagen;
import com.example.demo.model.Instituto;
import com.example.demo.model.OfertaImagen;
import com.example.demo.model.InstitutoLogo;
import com.example.demo.model.UsuarioImagen;
import com.example.demo.service.ImagenService;
import com.example.demo.service.InstitutoService;
import com.example.demo.service.InstitutoLogoService;
import com.example.demo.service.OfertaImagenService;
import com.example.demo.service.UsuarioImagenService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class ImagenController {

    @Autowired
    private ImagenService imagenService;
    
    @Autowired
    private InstitutoService institutoService;
    
    @Autowired
    private OfertaImagenService ofertaImagenService;
    
    @Autowired
    private UsuarioImagenService usuarioImagenService;
    
    @Autowired
    private InstitutoLogoService institutoLogoService;

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

    /**
     * Sirve las imágenes de perfil de los usuarios directamente desde el disco.
     * Esto soluciona problemas de archivos que no se ven inmediatamente después de subirlos.
     */
    @GetMapping("/img/usuarios/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> servirImagenUsuario(@PathVariable String filename) {
        try {
            // Ruta consistente con AlumnoController
            Path path = Paths.get("src/main/resources/static/img/usuarios").resolve(filename);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(path);
                if (contentType == null) {
                    contentType = "image/jpeg"; // Fallback
                }
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                // Intento fallback: buscar en target/classes por si acaso
                Path pathTarget = Paths.get("target/classes/static/img/usuarios").resolve(filename);
                Resource resourceTop = new UrlResource(pathTarget.toUri());
                 if (resourceTop.exists() || resourceTop.isReadable()) {
                    String contentType = Files.probeContentType(pathTarget);
                    if (contentType == null) contentType = "image/jpeg";
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resourceTop);
                 }

                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint alternativo para compatibilidad

    @GetMapping("/imagen/carrusel/{id}")
    public ResponseEntity<byte[]> obtenerImagenCarruselAlt(@PathVariable Long id) {
        return obtenerImagenCarrusel(id);
    }

    // Endpoint para servir las imágenes de ofertas académicas
    @GetMapping("/api/ofertas/imagen/{id}")
    public ResponseEntity<byte[]> obtenerImagenOferta(@PathVariable Long id) {
        Optional<OfertaImagen> imagen = ofertaImagenService.obtenerImagenOferta(id);
        
        if (imagen.isPresent()) {
            OfertaImagen img = imagen.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(img.getTipoMime()));
            headers.setContentLength(img.getTamano());
            headers.setCacheControl("max-age=3600"); // Cache por 1 hora
            
            return new ResponseEntity<>(img.getDatos(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint para servir las imágenes de perfil de usuarios almacenadas en BD
    @GetMapping("/api/usuarios/imagen/{id}")
    public ResponseEntity<byte[]> obtenerImagenUsuario(@PathVariable Long id) {
        Optional<UsuarioImagen> imagen = usuarioImagenService.obtenerImagenUsuario(id);
        
        if (imagen.isPresent()) {
            UsuarioImagen img = imagen.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(img.getTipoMime()));
            headers.setContentLength(img.getTamano());
            headers.setCacheControl("max-age=3600");
            
            return new ResponseEntity<>(img.getDatos(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint para servir el logo institucional desde BD
    @GetMapping("/api/instituto/logo/{id}")
    public ResponseEntity<byte[]> obtenerLogoInstituto(@PathVariable Long id) {
        Optional<InstitutoLogo> logo = institutoLogoService.obtenerPorId(id);
        if (logo.isPresent()) {
            InstitutoLogo img = logo.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(img.getTipoMime()));
            headers.setContentLength(img.getTamanio());
            headers.setCacheControl("max-age=3600");
            return new ResponseEntity<>(img.getDatos(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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
