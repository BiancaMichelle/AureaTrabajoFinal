package com.example.demo;

import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UsuarioRepository;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoApplication(RoleRepository roleRepository,
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
        Stream.of("ROLE_ADMIN", "ROLE_ESTUDIANTE", "ROLE_DOCENTE")
              .forEach(name -> {
                  if (roleRepository.findByName(name).isEmpty()) {
                      roleRepository.save(new Role(null, name));
                  }
              });

        // Crear usuario admin por defecto
        if (usuarioRepository.findByUsername("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                                     .orElseThrow();
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            admin.getRoles().add(adminRole);
            usuarioRepository.save(admin);
        }
    }
}

