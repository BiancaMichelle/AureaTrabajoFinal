package com.example.demo.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Archivo;
import com.example.demo.model.Carpeta;
import com.example.demo.model.Material;
import com.example.demo.model.Modulo;
import com.example.demo.repository.ArchivoRepository;
import com.example.demo.repository.CarpetaRepository;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.repository.ModuloRepository;

@Controller
@RequestMapping("/carpeta")
public class CarpetaController {

    private final CarpetaRepository carpetaRepository;
    private final ArchivoRepository archivoRepository;
    private final ModuloRepository moduloRepository;
    private final MaterialRepository materialRepository;

    public CarpetaController(CarpetaRepository carpetaRepository, ArchivoRepository archivoRepository,
            ModuloRepository moduloRepository, MaterialRepository materialRepository) {
        this.carpetaRepository = carpetaRepository;
        this.archivoRepository = archivoRepository;
        this.moduloRepository = moduloRepository;
        this.materialRepository = materialRepository;
    }

    @PostMapping("/crear")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearCarpeta(@RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam UUID moduloId) {
        try {
            Modulo modulo = moduloRepository.findById(moduloId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado"));

            Carpeta carpeta = new Carpeta(titulo, descripcion, modulo);
            carpetaRepository.save(carpeta);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Carpeta creada exitosamente");
            response.put("carpetaId", carpeta.getIdActividad());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al crear carpeta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{carpetaId}/archivos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerArchivos(@PathVariable Long carpetaId) {
        List<Archivo> archivos = archivoRepository.findByCarpetaIdActividad(carpetaId);

        List<Map<String, Object>> archivosInfo = archivos.stream().map(archivo -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", archivo.getIdArchivo());
            info.put("nombre", archivo.getNombre());
            info.put("tipoMime", archivo.getTipoMime());
            info.put("tamano", archivo.getTamano());
            info.put("fechaSubida", archivo.getFechaSubida().toString());
            return info;
        }).toList();

        return ResponseEntity.ok(archivosInfo);
    }

    @PostMapping("/{carpetaId}/subir")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirArchivo(@PathVariable Long carpetaId,
            @RequestParam("archivo") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No se seleccionó archivo"));
            }

            // Validar que sea PDF
            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Solo se permiten archivos PDF"));
            }

            Carpeta carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new RuntimeException("Carpeta no encontrada"));

            Archivo archivo = new Archivo();
            archivo.setNombre(file.getOriginalFilename());
            archivo.setTipoMime(file.getContentType());
            archivo.setTamano(file.getSize());
            archivo.setContenido(file.getBytes());
            archivo.setFechaSubida(LocalDateTime.now());
            archivo.setCarpeta(carpeta);

            archivoRepository.save(archivo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Archivo subido exitosamente");
            response.put("archivoId", archivo.getIdArchivo());
            response.put("nombre", archivo.getNombre());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al leer el archivo: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al subir archivo: " + e.getMessage()));
        }
    }

    @GetMapping("/archivo/{archivoId}/descargar")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long archivoId) {
        try {
            Archivo archivo = archivoRepository.findById(archivoId)
                    .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(archivo.getTipoMime()));
            headers.setContentDispositionFormData("attachment", archivo.getNombre());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(archivo.getContenido());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/archivo/{archivoId}/eliminar")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarArchivo(@PathVariable Long archivoId) {
        try {
            archivoRepository.deleteById(archivoId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Archivo eliminado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al eliminar archivo"));
        }
    }

    @PostMapping("/{carpetaId}/material")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'ADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearMaterial(@PathVariable Long carpetaId,
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> files) {
        try {
            Carpeta carpeta = carpetaRepository.findById(carpetaId)
                    .orElseThrow(() -> new RuntimeException("Carpeta no encontrada"));

            Material material = new Material();
            material.setTitulo(titulo);
            material.setDescripcion(descripcion);
            material.setFechaCreacion(LocalDateTime.now());
            material.setVisibilidad(true);
            material.setModulo(carpeta.getModulo());
            material.setCarpeta(carpeta);

            // Si hay archivos, guardarlos
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        Archivo archivo = new Archivo();
                        archivo.setNombre(file.getOriginalFilename());
                        archivo.setTipoMime(file.getContentType());
                        archivo.setTamano(file.getSize());
                        archivo.setContenido(file.getBytes());
                        archivo.setFechaSubida(LocalDateTime.now());
                        archivo.setMaterial(material);
                        archivo.setCarpeta(carpeta);
                        
                        material.getArchivos().add(archivo);
                    }
                }
            }

            materialRepository.save(material);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Material creado exitosamente");
            response.put("materialId", material.getIdActividad());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al procesar los archivos: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al crear material: " + e.getMessage()));
        }
    }

    @GetMapping("/{carpetaId}/materiales")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerMateriales(@PathVariable Long carpetaId) {
        List<Material> materiales = materialRepository.findByCarpetaIdActividad(carpetaId);

        List<Map<String, Object>> materialesInfo = materiales.stream().map(material -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", material.getIdActividad());
            info.put("titulo", material.getTitulo());
            info.put("descripcion", material.getDescripcion());
            info.put("fechaCreacion", material.getFechaCreacion().toString());
            
            // Incluir información de los archivos
            List<Map<String, Object>> archivosInfo = material.getArchivos().stream().map(archivo -> {
                Map<String, Object> aInfo = new HashMap<>();
                aInfo.put("id", archivo.getIdArchivo());
                aInfo.put("nombre", archivo.getNombre());
                aInfo.put("tipoMime", archivo.getTipoMime());
                aInfo.put("tamano", archivo.getTamano());
                return aInfo;
            }).toList();
            
            info.put("archivos", archivosInfo);
            
            return info;
        }).toList();

        return ResponseEntity.ok(materialesInfo);
    }
}
