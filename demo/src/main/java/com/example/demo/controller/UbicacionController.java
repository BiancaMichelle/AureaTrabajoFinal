package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Ciudad;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.repository.CiudadRepository;
import com.example.demo.repository.PaisRepository;
import com.example.demo.repository.ProvinciaRepository;
import com.example.demo.service.LocacionAPIService;

@Controller
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final LocacionAPIService locacionApiService;
    private final PaisRepository paisRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CiudadRepository ciudadRepository;

    public UbicacionController(LocacionAPIService locacionApiService,
                              PaisRepository paisRepository,
                              ProvinciaRepository provinciaRepository,
                              CiudadRepository ciudadRepository) {
        this.locacionApiService = locacionApiService;
        this.paisRepository = paisRepository;
        this.provinciaRepository = provinciaRepository;
        this.ciudadRepository = ciudadRepository;
    }

    // ✅ Endpoint para guardar ubicaciones
    @PostMapping("/guardar")
    @ResponseBody
    public String guardarUbicaciones(
            @RequestParam String paisCodigo,
            @RequestParam String provinciaCodigo,
            @RequestParam Long ciudadId,
            @RequestParam("_csrf") String csrfToken) { // ✅ Recibir el token CSRF
        
        System.out.println("📍 Guardando ubicaciones (Improved):");
        System.out.println("   - País: " + paisCodigo);
        System.out.println("   - Provincia: " + provinciaCodigo);
        System.out.println("   - Ciudad ID: " + ciudadId);
        
        try {
            // 1. Buscar o crear País
            Pais pais = paisRepository.findByCodigo(paisCodigo).orElse(null);
            if (pais == null) {
                System.out.println("🌎 Creando nuevo país: " + paisCodigo);
                try {
                    // Intento 1: Obtención directa (Más eficiente)
                    Pais paisApi = locacionApiService.obtenerPaisPorCodigo(paisCodigo);
                    
                    if (paisApi != null) {
                        pais = new Pais();
                        pais.setCodigo(paisApi.getCodigo());
                        pais.setNombre(paisApi.getNombre());
                        System.out.println("✅ País obtenido de API (directo): " + pais.getNombre());
                    } else {
                        // Intento 2: Búsqueda en lista (Fallback)
                        List<Pais> paises = locacionApiService.obtenerTodosPaises();
                        for (Pais p : paises) {
                            if (paisCodigo.equalsIgnoreCase(p.getCodigo())) {
                                pais = new Pais();
                                pais.setCodigo(p.getCodigo());
                                pais.setNombre(p.getNombre());
                                System.out.println("✅ País encontrado en lista API: " + pais.getNombre());
                                break;
                            }
                        }
                    }

                    if (pais == null) {
                        throw new RuntimeException("País no encontrado en API");
                    }
                    
                    pais.setId(null); 
                    pais = paisRepository.save(pais);
                    
                } catch (Exception e) {
                    System.out.println("⚠️ Error obteniendo país de API (" + e.getMessage() + "). Usando fallback.");
                    pais = new Pais();
                    pais.setCodigo(paisCodigo);
                    pais.setNombre("País " + paisCodigo);
                    pais = paisRepository.save(pais);
                }
            } else {
                System.out.println("✅ País ya existe en BD: " + pais.getNombre());
            }

            // 2. Buscar o crear Provincia
            Provincia provincia = provinciaRepository.findByCodigo(provinciaCodigo).orElse(null);
            if (provincia == null) {
                System.out.println("🏙️ Creando nueva provincia: " + provinciaCodigo);
                try {
                    // Intento 1: Obtención directa
                    Provincia provinciaApi = locacionApiService.obtenerProvinciaPorCodigo(paisCodigo, provinciaCodigo);
                    
                    if (provinciaApi != null) {
                        provincia = new Provincia();
                        provincia.setCodigo(provinciaApi.getCodigo());
                        provincia.setNombre(provinciaApi.getNombre());
                        provincia.setPais(pais);
                        System.out.println("✅ Provincia obtenida de API (directo): " + provincia.getNombre());
                    } else {
                        // Intento 2: Búsqueda en lista
                        List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(paisCodigo);
                        for (Provincia p : provincias) {
                            if (provinciaCodigo.equalsIgnoreCase(p.getCodigo())) {
                                provincia = new Provincia();
                                provincia.setCodigo(p.getCodigo());
                                provincia.setNombre(p.getNombre());
                                provincia.setPais(pais);
                                System.out.println("✅ Provincia encontrada en lista API: " + provincia.getNombre());
                                break;
                            }
                        }
                    }

                    if (provincia == null) {
                        throw new RuntimeException("Provincia no encontrada en API");
                    }
                    
                    provincia.setId(null);
                    provincia.setPais(pais);
                    provincia = provinciaRepository.save(provincia);
                    
                } catch (Exception e) {
                    System.out.println("⚠️ Error obteniendo provincia de API (" + e.getMessage() + "). Usando fallback.");
                    provincia = new Provincia();
                    provincia.setCodigo(provinciaCodigo);
                    provincia.setNombre("Provincia " + provinciaCodigo);
                    provincia.setPais(pais);
                    provincia = provinciaRepository.save(provincia);
                }
            } else {
                if (provincia.getPais() == null) {
                    provincia.setPais(pais);
                    provincia = provinciaRepository.save(provincia);
                }
                System.out.println("✅ Provincia ya existe en BD: " + provincia.getNombre());
            }

            // 3. Buscar o crear Ciudad
            Ciudad ciudad = ciudadRepository.findById(ciudadId).orElse(null);
            if (ciudad == null) {
                System.out.println("🏡 Creando nueva ciudad: " + ciudadId);
                try {
                    List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCodigo, provinciaCodigo);
                    for (Ciudad c : ciudades) {
                        if (ciudadId.equals(c.getId())) {
                            ciudad = new Ciudad();
                            ciudad.setId(c.getId());
                            ciudad.setNombre(c.getNombre());
                            ciudad.setProvincia(provincia);
                            System.out.println("✅ Ciudad encontrada en API: " + ciudad.getNombre());
                            break;
                        }
                    }
                    
                    if (ciudad == null) {
                        ciudad = new Ciudad();
                        ciudad.setId(ciudadId);
                        ciudad.setNombre("Ciudad " + ciudadId);
                        ciudad.setProvincia(provincia);
                        System.out.println("⚠️ Ciudad no encontrada en lista API. Creando con ID.");
                    }
                    
                    ciudad.setProvincia(provincia); // Asegurar relación
                    ciudad = ciudadRepository.save(ciudad);
                    
                } catch (Exception e) {
                    System.out.println("⚠️ Error obteniendo ciudad de API (" + e.getMessage() + "). Usando fallback.");
                    ciudad = new Ciudad();
                    ciudad.setId(ciudadId);
                    ciudad.setNombre("Ciudad " + ciudadId);
                    ciudad.setProvincia(provincia);
                    ciudad = ciudadRepository.save(ciudad);
                }
            } else {
                 if (ciudad.getProvincia() == null) {
                    ciudad.setProvincia(provincia);
                    ciudad = ciudadRepository.save(ciudad);
                }
                System.out.println("✅ Ciudad ya existe en BD: " + ciudad.getNombre());
            }

            String mensaje = String.format("Ubicaciones guardadas: %s - %s - %s", 
                pais.getNombre(), provincia.getNombre(), ciudad.getNombre());
            
            System.out.println("✅ " + mensaje);
            return mensaje;

        } catch (Exception e) {
            System.out.println("❌ Error CRÍTICO guardando ubicaciones: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/paises")
    public ResponseEntity<List<Pais>> obtenerTodosPaises() {
        try {
            List<Pais> paises = locacionApiService.obtenerTodosPaises();
            return ResponseEntity.ok(paises);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provincias/{paisCode}")
    public ResponseEntity<List<Provincia>> obtenerProvincias(@PathVariable String paisCode) {
        try {
            System.out.println("🌍 Solicitando provincias para país: " + paisCode);
            List<Provincia> provincias = locacionApiService.obtenerProvinciasPorPais(paisCode);
            
            System.out.println("✅ Provincias encontradas: " + provincias.size());
            
            // Log de las primeras 3 provincias para debug
            if (!provincias.isEmpty()) {
                System.out.println("📋 Primeras provincias:");
                provincias.stream().limit(3).forEach(p -> 
                    System.out.println("   - " + p.getNombre() + " (Código: " + p.getCodigo() + ", ID: " + p.getId() + ")")
                );
            }
            
            return ResponseEntity.ok(provincias);
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo provincias para " + paisCode + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ciudades/{paisCode}/{provinciaCode}")
    public ResponseEntity<List<Ciudad>> obtenerCiudades(@PathVariable String paisCode, 
                                                       @PathVariable String provinciaCode) {
        try {
            List<Ciudad> ciudades = locacionApiService.obtenerCiudadesPorProvincia(paisCode, provinciaCode);
            return ResponseEntity.ok(ciudades);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}