package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RecuperarContraseñaService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public RecuperarContraseñaService(UsuarioRepository usuarioRepository, 
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
        
        // Generar contraseña temporal
        String passwordTemp = generarPasswordTemporal();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiracion = LocalDateTime.now().plusHours(24);
        
        // Guardar datos temporales en el usuario
        usuario.setPasswordTemporal(passwordEncoder.encode(passwordTemp));
        usuario.setTokenRecuperacion(token);
        usuario.setExpiracionToken(expiracion);
        
        usuarioRepository.save(usuario);
        
        // Enviar correo
        enviarEmailRecuperacion(usuario, passwordTemp, token);
        
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
     * Envía email con la contraseña temporal y enlace de confirmación
     */
    private void enviarEmailRecuperacion(Usuario usuario, String passwordTemp, String token) {
        try {
            String subject = "Recuperación de Contraseña - Aurea";
            String enlaceConfirmacion = "http://localhost:8080/recuperacion/confirmar?token=" + token;
            
            String body = String.format(
                "Estimado/a %s %s,\n\n" +
                "Hemos recibido una solicitud para recuperar tu contraseña.\n\n" +
                "Tu contraseña temporal es: %s\n\n" +
                "Para confirmar el cambio de contraseña, haz clic en el siguiente enlace:\n" +
                "%s\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Si no solicitaste este cambio, ignora este correo.\n\n" +
                "Saludos cordiales,\n" +
                "Equipo Aurea",
                usuario.getNombre(), usuario.getApellido(), passwordTemp, enlaceConfirmacion
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
    
    /**
     * Limpia los datos temporales de recuperación
     */
    private void limpiarDatosRecuperacion(Usuario usuario) {
        usuario.setPasswordTemporal(null);
        usuario.setTokenRecuperacion(null);
        usuario.setExpiracionToken(null);
    }
}