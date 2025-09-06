package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Institucion;
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
        // 1. Manejar País
        if (alumno.getPais() != null && alumno.getPais().getCodigo() != null) {
            Pais pais = paisRepository.findByCodigo(alumno.getPais().getCodigo())
                .orElseGet(() -> {
                    // Crear nuevo país si no existe
                    Pais nuevoPais = new Pais();
                    nuevoPais.setCodigo(alumno.getPais().getCodigo());
                    nuevoPais.setNombre("Nombre temporal"); // Puedes obtener el nombre de la API si lo necesitas
                    return paisRepository.save(nuevoPais);
                });
            alumno.setPais(pais);
        }

        // 2. Manejar Provincia
        if (alumno.getProvincia() != null && alumno.getProvincia().getCodigo() != null) {
            Provincia provincia = provinciaRepository.findByCodigo(alumno.getProvincia().getCodigo())
                .orElseGet(() -> {
                    Provincia nuevaProvincia = new Provincia();
                    nuevaProvincia.setCodigo(alumno.getProvincia().getCodigo());
                    nuevaProvincia.setNombre("Nombre temporal");
                    nuevaProvincia.setPais(alumno.getPais()); // Asignar el país ya gestionado
                    return provinciaRepository.save(nuevaProvincia);
                });
            alumno.setProvincia(provincia);
        }

        // 3. Manejar Ciudad
        if (alumno.getCiudad() != null && alumno.getCiudad().getId() != null) {
            Ciudad ciudad = ciudadRepository.findById(alumno.getCiudad().getId())
                .orElseGet(() -> {
                    Ciudad nuevaCiudad = new Ciudad();
                    nuevaCiudad.setId(alumno.getCiudad().getId());
                    nuevaCiudad.setNombre("Nombre temporal");
                    nuevaCiudad.setProvincia(alumno.getProvincia()); // Asignar la provincia ya gestionada
                    return ciudadRepository.save(nuevaCiudad);
                });
            alumno.setCiudad(ciudad);
        }

        // 4. Manejar Institución
        if (alumno.getColegioEgreso() != null && alumno.getColegioEgreso().getId() != null) {
            Institucion institucion = institucionRepository.findById(alumno.getColegioEgreso().getId())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada con ID: " + alumno.getColegioEgreso().getId()));
            alumno.setColegioEgreso(institucion);
        }

        // 5. Encriptar contraseña
        alumno.setContraseña(passwordEncoder.encode(alumno.getContraseña()));

        // 6. Guardar alumno
        alumnoRepository.save(alumno);
    }
}