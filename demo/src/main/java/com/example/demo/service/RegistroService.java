package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Alumno;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.repository.AlumnoRepository;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.InstitucionRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;

@Service
public class RegistroService {
    private final AlumnoRepository alumnoRepository;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocacionAPIService locacionApiService;

    public RegistroService(AlumnoRepository alumnoRepository,
                          PaisRepository paisRepository,
                          ProvinciaRepository provinciaRepository,
                          CiudadRepository ciudadRepository,
                          InstitucionRepository institucionRepository,
                          PasswordEncoder passwordEncoder,
                          LocacionAPIService locacionApiService) {
        this.alumnoRepository = alumnoRepository;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
        this.institucionRepository = institucionRepository;
        this.passwordEncoder = passwordEncoder;
        this.locacionApiService = locacionApiService;
    }

    public void registrarUsuario(Alumno alumno, String paisCodigo, String provinciaCodigo, Long ciudadId) {
        System.out.println("🔍 Iniciando registro para: " + alumno.getNombre());
        System.out.println("📍 Parámetros de ubicación:");
        System.out.println("   - País código: " + paisCodigo);
        System.out.println("   - Provincia código: " + provinciaCodigo);
        System.out.println("   - Ciudad ID: " + ciudadId);
        
        try {
            // 1. Verificar si el DNI ya existe
            if (alumnoRepository.existsByDni(alumno.getDni())) {
                throw new RuntimeException("El DNI ya está registrado");
            }

            // 2. Verificar si el email ya existe
            if (alumnoRepository.existsByCorreo(alumno.getCorreo())) {
                throw new RuntimeException("El correo electrónico ya está registrado");
            }

            // 3. OBTENER Y GUARDAR PAÍS desde la API
            Pais pais = obtenerOPersistirPais(paisCodigo);
            alumno.setPais(pais);

            // 4. OBTENER Y GUARDAR PROVINCIA desde la API
            Provincia provincia = obtenerOPersistirProvincia(provinciaCodigo, pais);
            alumno.setProvincia(provincia);

            // 5. OBTENER Y GUARDAR CIUDAD desde la API
            Ciudad ciudad = obtenerOPersistirCiudad(ciudadId, provincia);
            alumno.setCiudad(ciudad);

            // 6. Información del colegio
            System.out.println("🏫 Colegio de egreso: " + alumno.getColegioEgreso());
            System.out.println("📅 Año de egreso: " + alumno.getAñoEgreso());

            // 7. Encriptar contraseña
            System.out.println("🔐 Encriptando contraseña");
            String contraseñaPlana = alumno.getContraseña();
            alumno.setContraseña(passwordEncoder.encode(alumno.getContraseña()));
            System.out.println("✅ Contraseña encriptada");

            // 8. Establecer estado por defecto
            alumno.setEstado(true);
            alumno.setEstadoCuenta(true);

            // 9. Guardar alumno
            System.out.println("💾 Guardando alumno en la base de datos...");
            Alumno alumnoGuardado = alumnoRepository.save(alumno);
            System.out.println("✅ Registro completado exitosamente. ID: " + alumnoGuardado.getId());
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage(), e);
        }
    }

    private Pais obtenerOPersistirPais(String paisCodigo) {
        System.out.println("🌎 Buscando o creando país con código: " + paisCodigo);
        
        // Primero buscar en la base de datos con una transacción separada
        Optional<Pais> paisExistente = buscarPaisEnBaseDeDatos(paisCodigo);
        if (paisExistente.isPresent()) {
            System.out.println("✅ País encontrado en BD: " + paisExistente.get().getNombre());
            return paisExistente.get();
        }
        
        // Si no existe, obtener de la API y guardar
        try {
            System.out.println("🔄 Obteniendo país desde API...");
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            Optional<Pais> paisFromApi = paises.stream()
                .filter(p -> paisCodigo.equals(p.getCodigo()))
                .findFirst();
                
            if (paisFromApi.isPresent()) {
                Pais pais = paisFromApi.get();
                System.out.println("✅ País obtenido de API: " + pais.getNombre());
                return guardarPaisSafely(pais);
            } else {
                throw new RuntimeException("País no encontrado en API con código: " + paisCodigo);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo país desde API: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Pais> buscarPaisEnBaseDeDatos(String paisCodigo) {
        try {
            return paisRepository.findByCodigo(paisCodigo);
        } catch (Exception e) {
            System.out.println("⚠️ Error buscando país en BD: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pais guardarPaisSafely(Pais pais) {
        try {
            // Intentar guardar, si falla por duplicado, buscar el existente
            return paisRepository.save(pais);
        } catch (DataIntegrityViolationException | JpaSystemException e) {
            System.out.println("⚠️ País ya existe, buscando en BD...");
            return paisRepository.findByCodigo(pais.getCodigo())
                .orElseThrow(() -> new RuntimeException("Error guardando país: " + e.getMessage()));
        } catch (Exception e) {
            System.out.println("⚠️ Error guardando país: " + e.getMessage());
            // Si hay otro error, intentar recuperar el existente
            return paisRepository.findByCodigo(pais.getCodigo())
                .orElseThrow(() -> new RuntimeException("Error guardando país: " + e.getMessage()));
        }
    }
    

    private Provincia obtenerOPersistirProvincia(String provinciaCodigo, Pais pais) {
        System.out.println("🏙️ Buscando o creando provincia con código: " + provinciaCodigo);
        
        // Primero buscar en la base de datos
        Optional<Provincia> provinciaExistente = provinciaRepository.findByCodigo(provinciaCodigo);
        if (provinciaExistente.isPresent()) {
            System.out.println("✅ Provincia encontrada en BD: " + provinciaExistente.get().getNombre());
            return provinciaExistente.get();
        }
        
        // Si no existe, obtener de la API y guardar
        try {
            System.out.println("🔄 Obteniendo provincia desde API...");
            List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(pais.getCodigo());
            Optional<Provincia> provinciaFromApi = provincias.stream()
                .filter(p -> provinciaCodigo.equals(p.getCodigo()))
                .findFirst();
                
            if (provinciaFromApi.isPresent()) {
                Provincia provincia = provinciaFromApi.get();
                provincia.setPais(pais); // Establecer relación con el país
                System.out.println("✅ Provincia obtenida de API: " + provincia.getNombre());
                return provinciaRepository.save(provincia);
            } else {
                throw new RuntimeException("Provincia no encontrada en API con código: " + provinciaCodigo);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo provincia desde API: " + e.getMessage());
        }
    }

    private Ciudad obtenerOPersistirCiudad(Long ciudadId, Provincia provincia) {
        System.out.println("🏡 Buscando o creando ciudad con ID: " + ciudadId);
        
        // Primero buscar en la base de datos
        Optional<Ciudad> ciudadExistente = ciudadRepository.findById(ciudadId);
        if (ciudadExistente.isPresent()) {
            System.out.println("✅ Ciudad encontrada en BD: " + ciudadExistente.get().getNombre());
            return ciudadExistente.get();
        }
        
        // Si no existe, obtener de la API y guardar
        try {
            System.out.println("🔄 Obteniendo ciudad desde API...");
            List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(
                provincia.getPais().getCodigo(), 
                provincia.getCodigo()
            );
            Optional<Ciudad> ciudadFromApi = ciudades.stream()
                .filter(c -> ciudadId.equals(c.getId()))
                .findFirst();
                
            if (ciudadFromApi.isPresent()) {
                Ciudad ciudad = ciudadFromApi.get();
                ciudad.setProvincia(provincia); // Establecer relación con la provincia
                System.out.println("✅ Ciudad obtenida de API: " + ciudad.getNombre());
                return ciudadRepository.save(ciudad);
            } else {
                throw new RuntimeException("Ciudad no encontrada en API con ID: " + ciudadId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo ciudad desde API: " + e.getMessage());
        }
    }
}