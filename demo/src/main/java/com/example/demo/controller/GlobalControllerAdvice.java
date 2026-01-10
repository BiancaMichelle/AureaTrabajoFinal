package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.demo.model.Instituto;
import com.example.demo.service.InstitutoService;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private InstitutoService institutoService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request) {
        // Agregar el instituto a todas las vistas para que puedan acceder a la información
        // en el footer y otras partes comunes
        Instituto instituto = institutoService.obtenerInstituto();
        model.addAttribute("instituto", instituto);
        
        // Agregar la URI actual para marcar la navegación activa
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("contextPath", request.getContextPath());
    }
}