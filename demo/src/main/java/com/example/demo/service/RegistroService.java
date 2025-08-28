package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Role;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class RegistroService {
    private UsuarioRepository usuarioRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder encoder;

    public RegistroService(UsuarioRepository usuarioRepository, RoleRepository roleRepository, PasswordEncoder encoder){
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
    }

    public Usuario registrarUsuario(String username, String rawPassword, String roleName) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Usuario ya existe");
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Rol inválido"));

        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.getRoles().add(role);
        return usuarioRepository.save(user);
    }

}
