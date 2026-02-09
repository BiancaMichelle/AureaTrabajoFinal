package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.RecuperarContraseñaService;

@Controller
public class LoginController {
    
    @Value("${app.base-url}")
    private String baseUrl;

    private final RecuperarContraseñaService passwordRecoveryService;
    private final UsuarioRepository usuarioRepository;
    
    // Inyectar el servicio en el constructor
    public LoginController(RecuperarContraseñaService passwordRecoveryService,
                           UsuarioRepository usuarioRepository) {
        this.passwordRecoveryService = passwordRecoveryService;
        this.usuarioRepository = usuarioRepository;
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
        System.out.println("Procesando recuperacion para: " + dniOCorreo);

        boolean existeUsuario = usuarioRepository.findByDniOrCorreo(dniOCorreo, dniOCorreo).isPresent();
        if (!existeUsuario) {
            return "redirect:/login?mensaje=No encontramos un usuario con ese DNI o correo. Si aun no tienes cuenta, te recomendamos registrarte.&tipo=warning";
        }

        boolean solicitudExitosa = passwordRecoveryService.iniciarRecuperacionPassword(dniOCorreo);
        if (solicitudExitosa) {
            return "redirect:/login?mensaje=Se ha enviado un correo con instrucciones para recuperar tu clave de acceso.&tipo=success";
        }

        return "redirect:/login?mensaje=No pudimos iniciar la recuperacion. Intenta nuevamente en unos minutos.&tipo=error";
    }
}
