package com.example.demo.configSecurity;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    // @Override
    // public void run(String... args) throws Exception {
    //     // Crear rol ADMIN si no existe
    //     Rol adminRole = rolRepository.findByNombre("ADMIN").orElseGet(() -> {
    //         Rol newRole = new Rol();
    //         newRole.setNombre("ADMIN");
    //         return rolRepository.save(newRole);
    //     });

    //     // Crear usuario admin si no existe
    //     if (!usuarioRepository.findByDni("00000000").isPresent()) {
    //         Usuario adminUser = new Usuario();
    //         adminUser.setDni("00000000");
    //         adminUser.setNombre("Admin");
    //         adminUser.setApellido("User");
    //         adminUser.setCorreo("admin@aurea.com");
    //         adminUser.setContraseña(passwordEncoder.encode("admin"));
    //         Set<Rol> roles = new HashSet<>();
    //         roles.add(adminRole);
    //         adminUser.setRoles(roles);
    //         adminUser.setEstado(true); // Activo
    //         usuarioRepository.save(adminUser);
    //         System.out.println(">>> Usuario administrador creado con DNI 00000000 y contraseña admin");
    //     }
    // }
}