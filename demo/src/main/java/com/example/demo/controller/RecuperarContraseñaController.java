package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.RecuperarContraseñaService;

@Controller
@RequestMapping("/recuperacion")
public class RecuperarContraseñaController {
    private final RecuperarContraseñaService passwordRecoveryService;
    
    public RecuperarContraseñaController(RecuperarContraseñaService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }
    
    /**
     * Endpoint para confirmar la recuperación (desde el enlace del email)
     */
    @GetMapping("/confirmar")
    public String confirmarRecuperacion(@RequestParam("token") String token) {
        System.out.println("✅ Confirmando recuperación con token: " + token);
        
        boolean confirmacionExitosa = passwordRecoveryService.confirmarRecuperacion(token);
        
        if (confirmacionExitosa) {
            return "redirect:/login?mensaje=¡Clave de acceso actualizada exitosamente! Ya puedes iniciar con tu nueva clave.&tipo=success";
        } else {
            return "redirect:/login?mensaje=El enlace de recuperacion ha expirado o no es válido. Por favor, solicita uno nuevo.&tipo=error";
        }
    }
}