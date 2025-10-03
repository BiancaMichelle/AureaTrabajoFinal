package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;

@Service
@Transactional
public class RegistroService {
    private final AlumnoRepository alumnoRepository;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(AlumnoRepository alumnoRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          PasswordEncoder passwordEncoder) {
        this.alumnoRepository = alumnoRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registrarUsuario(Alumno alumno, String paisCodigo, String provinciaCodigo, Long ciudadId) {
        System.out.println("🔍 Iniciando registro para: " + alumno.getNombre());
        
        try {
            // 1. Verificar si el DNI ya existe
            if (alumnoRepository.existsByDni(alumno.getDni())) {
                throw new RuntimeException("El DNI ya está registrado");
            }

            // 2. Verificar si el email ya existe
            if (alumnoRepository.existsByCorreo(alumno.getCorreo())) {
                throw new RuntimeException("El correo electrónico ya está registrado");
            }

            // 3. BUSCAR UBICACIONES (ya deben estar guardadas del paso 2)
            Pais pais = paisRepository.findByCodigo(paisCodigo)
                .orElseThrow(() -> new RuntimeException("País no encontrado. Completa el paso de ubicación nuevamente."));
            
            Provincia provincia = provinciaRepository.findByCodigo(provinciaCodigo)
                .orElseThrow(() -> new RuntimeException("Provincia no encontrada. Completa el paso de ubicación nuevamente."));
            
            Ciudad ciudad = ciudadRepository.findById(ciudadId)
                .orElseThrow(() -> new RuntimeException("Ciudad no encontrada. Completa el paso de ubicación nuevamente."));

            // 4. ASIGNAR UBICACIONES AL ALUMNO
            alumno.setPais(pais);
            alumno.setProvincia(provincia);
            alumno.setCiudad(ciudad);

            System.out.println("📍 Ubicaciones asignadas:");
            System.out.println("   - País: " + pais.getNombre());
            System.out.println("   - Provincia: " + provincia.getNombre());
            System.out.println("   - Ciudad: " + ciudad.getNombre());

            // 5. Encriptar contraseña
            alumno.setContraseña(passwordEncoder.encode(alumno.getContraseña()));

            // 6. Establecer estado por defecto
            alumno.setEstado(true);
            alumno.setEstadoCuenta(true);

            // 7. Guardar alumno
            Alumno alumnoGuardado = alumnoRepository.save(alumno);
            System.out.println("✅ Registro completado. ID: " + alumnoGuardado.getId());
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }
}