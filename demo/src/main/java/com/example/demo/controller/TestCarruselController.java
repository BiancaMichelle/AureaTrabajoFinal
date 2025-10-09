package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class TestCarruselController {

    @GetMapping("/test-carrusel")
    @ResponseBody
    public String testForm() {
        return """
            <html>
            <head><title>Test Carrusel</title></head>
            <body>
                <h1>Test de Subida de Carrusel</h1>
                <form action="/test-carrusel-upload" method="POST" enctype="multipart/form-data">
                    <input type="file" name="imagenes" multiple accept="image/*" required>
                    <button type="submit">Subir Imágenes</button>
                </form>
            </body>
            </html>
            """;
    }

    @PostMapping("/test-carrusel-upload")
    @ResponseBody
    public String testUpload(@RequestParam("imagenes") MultipartFile[] imagenes) {
        System.out.println("=== TEST UPLOAD ===");
        System.out.println("Imágenes recibidas: " + (imagenes != null ? imagenes.length : "null"));
        
        if (imagenes != null) {
            for (int i = 0; i < imagenes.length; i++) {
                System.out.println("Imagen " + i + ": " + imagenes[i].getOriginalFilename() + 
                                 " - Tamaño: " + imagenes[i].getSize());
            }
        }
        
        return "Recibidas " + (imagenes != null ? imagenes.length : 0) + " imágenes. Ver logs en consola.";
    }
}