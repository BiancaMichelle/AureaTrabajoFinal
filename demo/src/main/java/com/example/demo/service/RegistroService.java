package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Alumno;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class RegistroService {
    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private PasswordEncoder encoder;

    public RegistroService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder encoder){
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.encoder = encoder;
    }

    /**
     * Registrar un nuevo alumno
     */
    public Alumno registrarUsuario(Alumno alumno) {
        // Encriptar contraseña
        alumno.setContraseña(encoder.encode(alumno.getContraseña()));

        // Asignar rol ALUMNO
        Rol rolAlumno = rolRepository.findByNombre("ALUMNO")
                .orElseThrow(() -> new RuntimeException("No existe el rol ALUMNO en la BD"));

        alumno.getRoles().add(rolAlumno);

        // Guardar en repositorio
        return usuarioRepository.save(alumno);
    }

}
