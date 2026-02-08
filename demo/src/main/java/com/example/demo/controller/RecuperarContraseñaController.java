package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.demo.service.RecuperarContraseñaService;
import com.example.demo.model.Usuario;

@Controller
@RequestMapping("/recuperacion")
public class RecuperarContraseñaController {
    private final RecuperarContraseñaService passwordRecoveryService;
    private final AuthenticationManager authenticationManager;
    
    public RecuperarContraseñaController(RecuperarContraseñaService passwordRecoveryService,
                                         AuthenticationManager authenticationManager) {
        this.passwordRecoveryService = passwordRecoveryService;
        this.authenticationManager = authenticationManager;
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

    /**
     * Muestra el formulario para restablecer la contraseña
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNueva(@RequestParam("token") String token,
                                         @RequestParam(value = "error", required = false) String error,
                                         Model model) {
        if (!passwordRecoveryService.validarToken(token)) {
            return "redirect:/login?mensaje=El enlace de recuperacion ha expirado o no es válido. Por favor, solicita uno nuevo.&tipo=error";
        }
        model.addAttribute("token", token);
        model.addAttribute("error", error);
        return "screens/resetPassword";
    }

    /**
     * Procesa el restablecimiento de contraseña
     */
    @PostMapping("/nueva")
    public String procesarNueva(@RequestParam("token") String token,
                                @RequestParam("passwordNueva") String passwordNueva,
                                @RequestParam("passwordConfirmar") String passwordConfirmar,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        if (!passwordNueva.equals(passwordConfirmar)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "screens/resetPassword";
        }
        try {
            Usuario usuario = passwordRecoveryService.restablecerConToken(token, passwordNueva);
            if (usuario != null) {
                Authentication auth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(usuario.getDni(), passwordNueva));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                SecurityContextHolder.setContext(context);
                new HttpSessionSecurityContextRepository().saveContext(context, request, response);
                return redirigirSegunRol(auth);
            }
            return "redirect:/login?mensaje=El enlace de recuperacion ha expirado o no es válido. Por favor, solicita uno nuevo.&tipo=error";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "screens/resetPassword";
        }
    }

    private String redirigirSegunRol(Authentication auth) {
        boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority()));
        if (esAdmin) return "redirect:/admin/panelAdmin";
        boolean esDocente = auth.getAuthorities().stream().anyMatch(a -> "DOCENTE".equals(a.getAuthority()));
        if (esDocente) return "redirect:/docente/mi-espacio";
        boolean esAlumno = auth.getAuthorities().stream().anyMatch(a -> "ALUMNO".equals(a.getAuthority()));
        if (esAlumno) return "redirect:/alumno/perfil";
        return "redirect:/";
    }
}
