package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    private final RolRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoApplication(RolRepository roleRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }   

    
    @Override
    public void run(String... args) throws Exception {
        // Crear roles si no existen
        for (String name : List.of("ADMIN", "ALUMNO", "DOCENTE")) {
            if (roleRepository.findByNombre(name).isEmpty()) {
                Rol rol = new Rol();
                rol.setNombre(name);
                rol.setDescripcion(name);
                roleRepository.save(rol);
            }
        }

        if (usuarioRepository.findByDni("01234567").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setDni("01234567");
            admin.setNombre("Super");
            admin.setApellido("Admin");
            admin.setFechaNacimiento(LocalDate.of(1990, 1, 1)); // Fecha que cumple +16 años
            admin.setGenero("masculino");
            admin.setCorreo("admin@demo.com");
            admin.setNumTelefono("1234567890"); // 10 dígitos
            admin.setContraseña(passwordEncoder.encode("Admin123")); // Cumple: 8 chars, mayúscula y minúscula
            
            // Campos de estado
            admin.setEstado(true);
            admin.setEstadoCuenta(true);
            
            // Asignar rol ADMIN
            Rol rolAdmin = roleRepository.findByNombre("ADMIN")
                                        .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            admin.getRoles().add(rolAdmin);

            // Guardar usuario
            usuarioRepository.save(admin);
        }
    }

    
}

