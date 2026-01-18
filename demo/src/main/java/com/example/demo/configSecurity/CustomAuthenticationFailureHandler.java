package com.example.demo.configSecurity;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UsuarioRepository usuarioRepository;

    public CustomAuthenticationFailureHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String dni = request.getParameter("dni"); // Obtener el DNI intentado
        String errorMessage = "Credenciales inválidas";
        String errorType = "bad_credentials";

        if (dni != null && !dni.isBlank()) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(dni);

            if (usuarioOpt.isEmpty()) {
                // Caso: DNI no registrado
                errorType = "not_found";
                errorMessage = "El DNI ingresado no se encuentra registrado, registrate en el sistema";
            } else {
                Usuario usuario = usuarioOpt.get();
                if (!usuario.isEstadoCuenta()) { // Asumiendo estadoCuenta = true es activo
                    // Caso: Usuario en baja / inhabilitado
                    errorType = "disabled";
                    errorMessage = "Su cuenta se encuentra dada de baja.";
                } else {
                    // Caso: Contraseña incorrecta (Usuario existe y está activo)
                    errorType = "bad_password";
                    errorMessage = "La contraseña ingresada es incorrecta.";
                }
            }
        }
        
        // Si el usuario está deshabilitado, Spring Security lanza DisabledException primero
        if (exception instanceof DisabledException) {
             errorType = "disabled";
             errorMessage = "Su cuenta se encuentra deshabilitada.";
        }

        // Redirigir con el tipo de error para mostrar el mensaje adecuado
        String redirectUrl = "/login?error=" + URLEncoder.encode(errorType, StandardCharsets.UTF_8) +
                            "&message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
