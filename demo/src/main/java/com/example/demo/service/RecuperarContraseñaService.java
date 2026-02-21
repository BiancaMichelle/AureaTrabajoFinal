package com.example.demo.service;

import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RecuperarContraseñaService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String baseUrl;
    
    public RecuperarContraseñaService(UsuarioRepository usuarioRepository, 
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService,
                                 @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Inicia el proceso de recuperación de contraseña
     */
    public boolean iniciarRecuperacionPassword(String dniOCorreo) {
        System.out.println("🔐 Iniciando recuperación para: " + dniOCorreo);
        
        // Buscar usuario por DNI o correo
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDniOrCorreo(dniOCorreo, dniOCorreo);
        
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Usuario no encontrado: " + dniOCorreo);
            return false; // Por seguridad, no revelamos si existe o no
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Generar token de recuperación
        String token = UUID.randomUUID().toString();
        LocalDateTime expiracion = LocalDateTime.now().plusHours(24);
        
        // Guardar datos temporales en el usuario
        usuario.setPasswordTemporal(null);
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionToken(expiracion);
        
        usuarioRepository.save(usuario);
        
        // Enviar correo
        enviarEmailRecuperacion(usuario, token);
        
        System.out.println("✅ Proceso de recuperación iniciado para: " + usuario.getCorreo());
        return true;
    }
    
    /**
     * Confirma la recuperación usando el token
     */
    public boolean confirmarRecuperacion(String token) {
        System.out.println("🔍 Validando token: " + token);
        
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenRecuperacion(token);
        
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Token no válido: " + token);
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Validar expiración
        if (usuario.getExpiracionToken().isBefore(LocalDateTime.now())) {
            System.out.println("❌ Token expirado para usuario: " + usuario.getCorreo());
            limpiarDatosRecuperacion(usuario);
            return false;
        }
        
        // Aplicar la contraseña temporal como contraseña principal
        usuario.setContraseña(usuario.getPasswordTemporal());
        limpiarDatosRecuperacion(usuario);
        
        usuarioRepository.save(usuario);
        
        // Enviar correo de confirmación
        enviarEmailConfirmacion(usuario);
        
        System.out.println("✅ Contraseña actualizada para: " + usuario.getCorreo());
        return true;
    }
    
    /**
     * Inicia el proceso de cambio de contraseña iniciado por el usuario
     * NO actualiza la contraseña inmediatamente, sino que sigue un flujo de confirmación por email
     */
    public boolean solicitarCambioContraseña(Usuario usuario, String passwordActual, String passwordNueva) {
        // 1. Verificar contraseña actual
        if (!passwordEncoder.matches(passwordActual, usuario.getContraseña())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        // 2. Validar complejidad de nueva contraseña
        if (!validarComplejidadPassword(passwordNueva)) {
            throw new IllegalArgumentException("La nueva contraseña no cumple con los requisitos: Mínimo 8 caracteres, una mayúscula, un número y un carácter especial.");
        }

        // 3. Generar token y guardar "password temporal" (la nueva propuesta)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiracion = LocalDateTime.now().plusHours(24);

        // AQUÍ está la clave: guardamos la nueva contraseña codificada en Temporal
        // Y NO tocamos la contraseña actual todavía.
        usuario.setPasswordTemporal(passwordEncoder.encode(passwordNueva));
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionToken(expiracion);

        usuarioRepository.save(usuario);

        // 4. Enviar email con el LINK de confirmación
        enviarEmailCambioPassword(usuario, token);

        return true;
    }

    private boolean validarComplejidadPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean tieneNumero = password.matches(".*\\d.*");
        boolean tieneMayuscula = password.matches(".*[A-Z].*");
        boolean tieneEspecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        return tieneNumero && tieneMayuscula && tieneEspecial;
    }

    private void enviarEmailCambioPassword(Usuario usuario, String token) {
        try {
            String subject = "Valida tu nueva contraseña - Aurea";
            String enlaceConfirmacion = construirEnlaceConfirmacion(token);

            String body = String.format(
                "Estimado/a %s %s,\n\n" +
                "Has solicitado cambiar tu contraseña de acceso desde tu perfil.\n\n" +
                "IMPORTANTE: Tu contraseña NO cambiará hasta que hagas clic en el siguiente enlace:\n" +
                "%s\n\n" +
                "Si haces clic, tu nueva contraseña quedará activa inmediatamente.\n" +
                "Este enlace expira en 24 horas.\n\n" +
                "Si no fuiste tú, simplemente ignora este correo y tu contraseña actual seguirá funcionando.\n\n" +
                "Saludos,\n" +
                "Equipo Aurea",
                usuario.getNombre(), usuario.getApellido(), enlaceConfirmacion
            );

            emailService.sendEmail(usuario.getCorreo(), subject, body);
            System.out.println("📧 Email de cambio de contraseña enviado a: " + usuario.getCorreo());
        } catch (Exception e) {
            System.out.println("❌ Error enviando email de cambio: " + e.getMessage());
            throw new RuntimeException("Error enviando email de confirmación", e);
        }
    }



    /**
     * Genera una contraseña temporal segura
     */
    private String generarPasswordTemporal() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        
        // Asegurar que cumple los requisitos: mayúscula, minúscula, número, símbolo
        password.append((char) (random.nextInt(26) + 'A')); // Mayúscula
        password.append((char) (random.nextInt(26) + 'a')); // Minúscula  
        password.append((char) (random.nextInt(10) + '0')); // Número
        password.append("!@#$%".charAt(random.nextInt(5))); // Símbolo
        
        // Completar hasta 12 caracteres
        for (int i = 4; i < 12; i++) {
            password.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        
        // Mezclar los caracteres
        char[] arrayPassword = password.toString().toCharArray();
        for (int i = arrayPassword.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = arrayPassword[i];
            arrayPassword[i] = arrayPassword[j];
            arrayPassword[j] = temp;
        }
        
        return new String(arrayPassword);
    }
    
    /**
     * Envía email con enlace para restablecer la contraseña
     */
    private void enviarEmailRecuperacion(Usuario usuario, String token) {
        try {
            String subject = "Recuperación de Contraseña - Aurea";
            String enlaceConfirmacion = construirEnlaceRestablecer(token);
            
            String body = String.format(
                "Estimado/a %s %s,\n\n" +
                "Hemos recibido una solicitud para recuperar tu contraseña.\n\n" +
                "Para establecer una nueva contraseña, haz clic en el siguiente enlace:\n" +
                "%s\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Si no solicitaste este cambio, ignora este correo.\n\n" +
                "Saludos cordiales,\n" +
                "Equipo Aurea",
                usuario.getNombre(), usuario.getApellido(), enlaceConfirmacion
            );
            
            emailService.sendEmail(usuario.getCorreo(), subject, body);
            System.out.println("📧 Email de recuperación enviado a: " + usuario.getCorreo());
            
        } catch (Exception e) {
            System.out.println("❌ Error enviando email de recuperación: " + e.getMessage());
            throw new RuntimeException("Error enviando email de recuperación", e);
        }
    }
    
    /**
     * Envía email confirmando el cambio de contraseña
     */
    private void enviarEmailConfirmacion(Usuario usuario) {
        try {
            String subject = "Contraseña Actualizada - Aurea";
            
            String body = String.format(
                "Estimado/a %s %s,\n\n" +
                "Tu contraseña ha sido actualizada exitosamente.\n\n" +
                "Ahora puedes iniciar sesión con tu nueva contraseña.\n\n" +
                "Si no realizaste este cambio, por favor contacta con soporte inmediatamente.\n\n" +
                "Saludos cordiales,\n" +
                "Equipo Aurea",
                usuario.getNombre(), usuario.getApellido()
            );
            
            emailService.sendEmail(usuario.getCorreo(), subject, body);
            System.out.println("📧 Email de confirmación enviado a: " + usuario.getCorreo());
            
        } catch (Exception e) {
            System.out.println("❌ Error enviando email de confirmación: " + e.getMessage());
        }
    }

    private String construirEnlaceConfirmacion(String token) {
        String base = baseUrl != null ? baseUrl.trim() : "http://localhost:8080";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String tokenEncoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return base + "/recuperacion/confirmar?token=" + tokenEncoded;
    }

    private String construirEnlaceRestablecer(String token) {
        String base = baseUrl != null ? baseUrl.trim() : "http://localhost:8080";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String tokenEncoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return base + "/recuperacion/nueva?token=" + tokenEncoded;
    }

    public boolean validarToken(String token) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenRecuperacion(token);
        if (usuarioOpt.isEmpty()) return false;
        Usuario usuario = usuarioOpt.get();
        return usuario.getExpiracionToken() != null && usuario.getExpiracionToken().isAfter(LocalDateTime.now());
    }

    public Usuario restablecerConToken(String token, String passwordNueva) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenRecuperacion(token);
        if (usuarioOpt.isEmpty()) return null;
        Usuario usuario = usuarioOpt.get();
        if (usuario.getExpiracionToken() == null || usuario.getExpiracionToken().isBefore(LocalDateTime.now())) {
            limpiarDatosRecuperacion(usuario);
            usuarioRepository.save(usuario);
            return null;
        }
        if (!validarComplejidadPassword(passwordNueva)) {
            throw new IllegalArgumentException("La nueva contraseña no cumple los requisitos de seguridad.");
        }
        usuario.setContraseña(passwordEncoder.encode(passwordNueva));
        limpiarDatosRecuperacion(usuario);
        usuarioRepository.save(usuario);
        enviarEmailConfirmacion(usuario);
        return usuario;
    }
    
    /**
     * Limpia los datos temporales de recuperación
     */
    private void limpiarDatosRecuperacion(Usuario usuario) {
        usuario.setPasswordTemporal(null);
        usuario.setTokenRecuperacion(null);
        usuario.setExpiracionToken(null);
    }
}
