package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class RegistroService {
    private final AlumnoRepository alumnoRepository;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroService(AlumnoRepository alumnoRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          InstitucionRepository institucionRepository,
                          PasswordEncoder passwordEncoder) {
        this.alumnoRepository = alumnoRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.institucionRepository = institucionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registrarUsuario(Alumno alumno) {
        System.out.println("🔍 Iniciando registro para: " + alumno.getNombre());
        
        try {
            // 1. Manejar País - BUSCAR EXISTENTE en lugar de crear nuevo
            if (alumno.getPais() != null && alumno.getPais().getCodigo() != null) {
                System.out.println("🌎 Buscando país con código: " + alumno.getPais().getCodigo());
                Pais pais = paisRepository.findByCodigo(alumno.getPais().getCodigo())
                    .orElseThrow(() -> new RuntimeException("País no encontrado: " + alumno.getPais().getCodigo()));
                alumno.setPais(pais);
            }

            // 2. Manejar Provincia - BUSCAR EXISTENTE
            if (alumno.getProvincia() != null && alumno.getProvincia().getCodigo() != null) {
                System.out.println("🏙️ Buscando provincia con código: " + alumno.getProvincia().getCodigo());
                Provincia provincia = provinciaRepository.findByCodigo(alumno.getProvincia().getCodigo())
                    .orElseThrow(() -> new RuntimeException("Provincia no encontrada: " + alumno.getProvincia().getCodigo()));
                alumno.setProvincia(provincia);
            }

            // 3. Manejar Ciudad (si existe)
            if (alumno.getCiudad() != null && alumno.getCiudad().getId() != null) {
                System.out.println("🏡 Buscando ciudad con ID: " + alumno.getCiudad().getId());
                Ciudad ciudad = ciudadRepository.findById(alumno.getCiudad().getId())
                    .orElseThrow(() -> new RuntimeException("Ciudad no encontrada con ID: " + alumno.getCiudad().getId()));
                alumno.setCiudad(ciudad);
            }

            // 4. TEMPORAL: No procesar institución - estamos usando campo simple
            System.out.println("🏫 Colegio de egreso: " + alumno.getColegioEgreso());

            // 5. Encriptar contraseña
            System.out.println("🔐 Encriptando contraseña");
            alumno.setContraseña(passwordEncoder.encode(alumno.getContraseña()));

            // 6. Guardar alumno
            System.out.println("💾 Guardando alumno en la base de datos");
            alumnoRepository.save(alumno);
            System.out.println("✅ Registro completado exitosamente");
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            throw e;
        }
    }
}