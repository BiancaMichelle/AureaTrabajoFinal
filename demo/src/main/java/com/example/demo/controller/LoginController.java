package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.RecuperarContraseñaService;

@Controller
public class LoginController {
    
    private final RecuperarContraseñaService passwordRecoveryService;
    
    // Inyectar el servicio en el constructor
    public LoginController(RecuperarContraseñaService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "mensaje", required = false) String mensaje,
                            @RequestParam(value = "tipo", required = false) String tipo,
                            Model model) {
        if (error != null) model.addAttribute("error", true);
        if (logout != null) model.addAttribute("logout", true);
        if (mensaje != null) {
            model.addAttribute("mensaje", mensaje);
            model.addAttribute("tipo", tipo);
        }
        return "screens/LogIn"; 
    }
    
    /**
     * Maneja el envío del formulario de recuperación
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("recovery-identifier") String dniOCorreo, 
                                     Model model) {
        System.out.println("🔐 Procesando recuperación para: " + dniOCorreo);
        
        boolean solicitudExitosa = passwordRecoveryService.iniciarRecuperacionPassword(dniOCorreo);
        
        if (solicitudExitosa) {
            return "redirect:/login?mensaje=Se ha enviado un correo con instrucciones para recuperar tu clave de acceso.&tipo=success";
        } else {
            // Por seguridad, mostramos el mismo mensaje aunque no exista
            return "redirect:/login?mensaje=Si el DNI o correo existen, recibirás un email con instrucciones.&tipo=info";
        }
    }
}