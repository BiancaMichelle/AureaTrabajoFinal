package com.example.demo;

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
            admin.setCorreo("admin@demo.com");
            admin.setContraseña(passwordEncoder.encode("1234"));
            admin.setEstado(true);
            admin.setEstadoCuenta(true);

            Rol rolAdmin = roleRepository.findByNombre("ADMIN")
                                        .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
            admin.getRoles().add(rolAdmin);

            usuarioRepository.save(admin);
        }
    }

    
}

