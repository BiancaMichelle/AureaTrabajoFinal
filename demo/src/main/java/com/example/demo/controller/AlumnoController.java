package com.example.demo.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.enums.EstadoCuota;
import com.example.demo.enums.EstadoPago;
import com.example.demo.enums.TipoGenero;
import com.example.demo.model.Alumno;
import com.example.demo.model.Curso;
import com.example.demo.model.Cuota;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.Modulo;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.ModuloRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.PagoRepository;
import com.example.demo.service.MercadoPagoService;

@Controller
@RequestMapping("/alumno")
public class AlumnoController {
    
    @Value("${app.base-url}")
    private String baseUrl;

    private static final Locale LOCALE_ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FORMATO_FECHA_RESUMEN = DateTimeFormatter.ofPattern("dd MMM yyyy", LOCALE_ES_AR);

    @GetMapping("/alumno")
    public String alumnoDashboard() {
        return "publico";
    }
    
    // Mi Espacio - Dashboard del alumno con calendario
    @GetMapping("/mi-espacio")
    public String miEspacio(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            model.addAttribute("alumno", alumno);
            
            return "alumno/mi-espacio";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mi-espacio: " + e.getMessage());
            model.addAttribute("error", "Error al cargar tu espacio");
            return "redirect:/";
        }
    }
    
    @PostMapping("/mis-pagos/cuotas/{cuotaId}/pagar")
    public String pagarCuota(@PathVariable Long cuotaId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String dni = principal.getName();

            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

                Long cuotaIdSeguro = Objects.requireNonNull(cuotaId, "cuotaId no puede ser nulo");

                Cuota cuota = cuotaRepository.findById(cuotaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));

            if (cuota.getInscripcion() == null || cuota.getInscripcion().getAlumno() == null
                    || !cuota.getInscripcion().getAlumno().getId().equals(alumno.getId())) {
                redirectAttributes.addFlashAttribute("error", "No tienes acceso a esta cuota");
                return "redirect:/alumno/mis-pagos";
            }

            BigDecimal saldoPendiente = calcularSaldoPendiente(cuota);
            if (saldoPendiente.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("success", "Esta cuota ya está al día");
                return "redirect:/alumno/mis-pagos";
            }

            ReferenceRequest request = construirReferenciaCuota(cuota, alumno, saldoPendiente);

            ResponseDTO response = mercadoPagoService.createPreferenceForCuota(
                    request,
                    alumno,
                    cuota.getInscripcion().getOferta(),
                    cuota);

            return "redirect:" + response.redirectUrl();

        } catch (Exception e) {
            System.err.println("❌ Error al iniciar pago de cuota: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "No pudimos iniciar el pago. Intenta nuevamente.");
            return "redirect:/alumno/mis-pagos";
        }
    }

    // Mis Pagos
    @GetMapping("/mis-pagos")
    public String misPagos(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));

            List<Inscripciones> inscripcionesAlumno = inscripcionRepository.findByAlumnoDni(dni);
            for (Inscripciones inscripcion : inscripcionesAlumno) {
                if (inscripcion.getIdInscripcion() != null) {
                    // Generar la primera cuota si no existe
                    if (cuotaRepository.findByInscripcionIdInscripcion(inscripcion.getIdInscripcion()).isEmpty()) {
                        mercadoPagoService.generarCuotasParaInscripcion(inscripcion);
                    } else {
                        // Verificar si corresponde generar la siguiente cuota
                        mercadoPagoService.generarSiguienteCuotaSiCorresponde(inscripcion);
                    }
                }
            }
            
            // Obtener TODOS los pagos (de inscripción y de cuotas)
            List<Pago> pagosRealizados = pagoRepository.findByUsuarioId(alumno.getId()).stream()
                .sorted(Comparator.comparing(this::obtenerFechaReferencia,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

            BigDecimal totalPagado = pagosRealizados.stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.COMPLETADO)
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDate hoy = LocalDate.now();

            // Obtener todas las cuotas del alumno
            List<Cuota> todasLasCuotas = cuotaRepository.findByUsuarioId(alumno.getId()).stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento,
                    Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());

            // Actualizar estados de cuotas vencidas
            todasLasCuotas.forEach(cuota -> {
                if (cuota.getEstado() == EstadoCuota.PENDIENTE && 
                    cuota.getFechaVencimiento() != null && 
                    cuota.getFechaVencimiento().isBefore(hoy)) {
                    cuota.setEstado(EstadoCuota.VENCIDA);
                    cuotaRepository.save(cuota);
                }
            });

            // Filtrar solo cuotas NO pagadas
            List<Cuota> cuotasActivas = todasLasCuotas.stream()
                .filter(c -> c.getEstado() != EstadoCuota.PAGADA && c.getEstado() != EstadoCuota.CANCELADA)
                .collect(Collectors.toList());

            // Encontrar la primera cuota pendiente o vencida para mostrar el botón de pago
            Cuota cuotaPendiente = cuotasActivas.stream()
                .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE || c.getEstado() == EstadoCuota.VENCIDA)
                .min(Comparator.comparing(Cuota::getFechaVencimiento, Comparator.nullsLast(LocalDate::compareTo)))
                .orElse(null);

            BigDecimal montoCuotaPendiente = cuotaPendiente != null
                ? calcularSaldoPendiente(cuotaPendiente)
                : BigDecimal.ZERO;

            BigDecimal totalPendiente = cuotasActivas.stream()
                .map(this::calcularSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            String proximoPagoTexto = cuotaPendiente != null 
                ? String.format("%s - %s",
                    formatearFecha(cuotaPendiente.getFechaVencimiento()),
                    formatearMoneda(calcularSaldoPendiente(cuotaPendiente)))
                : "Sin cuotas pendientes";

            model.addAttribute("alumno", alumno);
            model.addAttribute("pagos", pagosRealizados);
            model.addAttribute("cuotas", todasLasCuotas); // Todas las cuotas para el historial
            model.addAttribute("cuotaPendiente", cuotaPendiente);
            model.addAttribute("montoCuotaPendienteTexto", formatearMoneda(montoCuotaPendiente));
            model.addAttribute("totalPagadoTexto", formatearMoneda(totalPagado));
            model.addAttribute("totalPendienteTexto", formatearMoneda(totalPendiente));
            model.addAttribute("proximoPagoTexto", proximoPagoTexto);

            return "alumno/mis-pagos";
            
        } catch (Exception e) {
            System.out.println("❌ Error en mis-pagos: " + e.getMessage());
            model.addAttribute("error", "Error al cargar tus pagos");
            return "redirect:/";
        }
    }

    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;
    private final MercadoPagoService mercadoPagoService;
    private final PagoRepository pagoRepository;
    private final CuotaRepository cuotaRepository;

    public AlumnoController(OfertaAcademicaRepository ofertaAcademicaRepository,
                          InscripcionRepository inscripcionRepository,
                          UsuarioRepository usuarioRepository,
                          ModuloRepository moduloRepository,
                          MercadoPagoService mercadoPagoService,
                          PagoRepository pagoRepository,
                          CuotaRepository cuotaRepository) {
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.moduloRepository = moduloRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.pagoRepository = pagoRepository;
        this.cuotaRepository = cuotaRepository;
    }

    private BigDecimal calcularSaldoPendiente(Cuota cuota) {
        BigDecimal monto = Optional.ofNullable(cuota.getMonto()).orElse(BigDecimal.ZERO);
        BigDecimal pagado = Optional.ofNullable(cuota.getMontoPagado()).orElse(BigDecimal.ZERO);
        BigDecimal saldo = monto.subtract(pagado);
        return saldo.compareTo(BigDecimal.ZERO) > 0 ? saldo : BigDecimal.ZERO;
    }

    private LocalDateTime obtenerFechaReferencia(Pago pago) {
        if (pago == null) {
            return null;
        }
        return Optional.ofNullable(pago.getFechaAprobacion())
                .orElse(pago.getFechaPago());
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(LOCALE_ES_AR);
        return formatter.format(monto);
    }

    private String formatearFecha(LocalDate fecha) {
        if (fecha == null) {
            return "Sin fecha";
        }
        return FORMATO_FECHA_RESUMEN.format(fecha);
    }

    private ReferenceRequest construirReferenciaCuota(Cuota cuota, Usuario alumno, BigDecimal montoPendiente) {
        ReferenceRequest.PayerDTO payer = new ReferenceRequest.PayerDTO(
                alumno.getNombre() + " " + alumno.getApellido(),
                alumno.getCorreo());

        ReferenceRequest.BackUrlsDTO backUrls = new ReferenceRequest.BackUrlsDTO(
                baseUrl + "/pago/success",
                baseUrl + "/pago/failure",
                baseUrl + "/pago/pending");

        String titulo = String.format("Cuota %s - %s",
                cuota.getNumeroCuota() != null ? cuota.getNumeroCuota() : "",
                cuota.getInscripcion().getOferta().getNombre()).trim();

        ReferenceRequest.ItemDTO item = new ReferenceRequest.ItemDTO(
                "CUOTA-" + cuota.getIdCuota(),
                titulo,
                montoPendiente,
                1);

        return new ReferenceRequest(
                alumno.getId(),
                montoPendiente,
                payer,
                backUrls,
                List.of(item));
    }

    // Inscribirse a una oferta académica - Inscripción directa (sin pago)
    @PostMapping("/inscribirse/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            Long ofertaIdSeguro = Objects.requireNonNull(ofertaId, "ofertaId no puede ser nulo");
            System.out.println("📝 Iniciando proceso de inscripción directa para oferta: " + ofertaIdSeguro);
            
            // Buscar el usuario (alumno)
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Buscar la oferta
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + " (Tipo: " + oferta.getClass().getSimpleName() + ")");

            // Verificar si ya está inscrito
            List<Inscripciones> inscripcionesExistentes = inscripcionRepository.findByAlumnoDni(dni);
            boolean yaInscrito = inscripcionesExistentes.stream()
                    .anyMatch(ins -> ins.getOferta().getIdOferta().equals(ofertaIdSeguro));
            
            if (yaInscrito) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return "redirect:/publico";
            }

            // ✅ CREAR INSCRIPCIÓN DIRECTA usando Usuario
            Inscripciones nuevaInscripcion = new Inscripciones();
            nuevaInscripcion.setAlumno(usuario); // ✅ Ahora acepta Usuario
            nuevaInscripcion.setOferta(oferta);
            nuevaInscripcion.setEstadoInscripcion(true); // Inscripción activa
            nuevaInscripcion.setFechaInscripcion(LocalDate.now());
            
            inscripcionRepository.save(nuevaInscripcion);
            mercadoPagoService.generarCuotasParaInscripcion(nuevaInscripcion);
            
            System.out.println("✅ Inscripción creada exitosamente para " + usuario.getNombre());
            
            redirectAttributes.addFlashAttribute("success", 
                "¡Te has inscrito exitosamente a " + oferta.getNombre() + "!");
            
            return "redirect:/alumno/mis-ofertas";
            
        } catch (Exception e) {
            System.err.println("❌ Error al crear inscripción: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Error al procesar la inscripción: " + e.getMessage());
            return "redirect:/publico";
        }
    }

    // Ver mis ofertas académicas (inscripciones)
    @GetMapping("/mis-ofertas")
    public String misOfertasAcademicas(Principal principal, Model model) {
        try {
            String dni = principal.getName();
            System.out.println("🎓 Alumno accediendo a mis ofertas: " + dni);
            
            Usuario alumno = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
            
            // ✅ Buscar inscripciones del alumno
            List<Inscripciones> inscripciones = inscripcionRepository.findByAlumnoDni(dni);
            
            // ✅ Extraer TODAS las ofertas académicas (Cursos Y Formaciones)
            List<OfertaAcademica> ofertas = inscripciones.stream()
                    .map(Inscripciones::getOferta)
                    .collect(Collectors.toList());
            
            System.out.println("📊 Inscripciones encontradas: " + inscripciones.size());
            System.out.println("📊 Ofertas académicas: " + ofertas.size());
            
            // Debug: mostrar tipos de ofertas
            for (OfertaAcademica oferta : ofertas) {
                System.out.println("   - " + oferta.getClass().getSimpleName() + ": " + oferta.getNombre());
            }
            
            model.addAttribute("alumno", alumno);
            model.addAttribute("cursos", ofertas); // Mantener nombre "cursos" para compatibilidad con vista
            model.addAttribute("inscripciones", inscripciones);
            
            return "misOfertasAcademicas";
            
        } catch (Exception e) {
            System.out.println("❌ Error en misOfertasAcademicas (alumno): " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar tus cursos: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }

    @GetMapping("/aula/{ofertaId}")
    public String accederAlAula(@PathVariable Long ofertaId,
                              Authentication authentication,
                              Model model) {
        try {
            String dni = authentication.getName();
            Long ofertaIdSeguro = Objects.requireNonNull(ofertaId, "ofertaId no puede ser nulo");
            System.out.println("🔍 Accediendo al aula para oferta ID: " + ofertaIdSeguro + ", usuario: " + dni);
            
            // Buscar la inscripción del alumno en esta oferta
            Inscripciones inscripcion = inscripcionRepository.findByAlumnoDniAndOfertaId(dni, ofertaIdSeguro)
                    .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));

            System.out.println("✅ Inscripción encontrada ID: " + inscripcion.getIdInscripcion());

            // Verificar que la inscripción esté activa
            if (!inscripcion.getEstadoInscripcion()) {
                System.out.println("❌ Inscripción inactiva");
                model.addAttribute("error", "Esta inscripción no está activa");
                return "misOfertasAcademicas";
            }

            OfertaAcademica oferta = inscripcion.getOferta();
            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + ", tipo: " + oferta.getClass().getSimpleName());
            
            // Si es un curso o formación, cargar módulos y contenido
            if (oferta instanceof Curso || oferta instanceof Formacion) {
                System.out.println("🎓 Es un curso/formación: " + oferta.getNombre());
                
                // Verificar permisos de modificación (solo admin o docente de la oferta)
                boolean puedeModificar = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
                
                // Verificar si es docente según el tipo de oferta
                if (oferta instanceof Curso) {
                    Curso curso = (Curso) oferta;
                    puedeModificar = puedeModificar || (curso.getDocentes() != null && curso.getDocentes().stream()
                            .anyMatch(docente -> docente.getDni().equals(dni)));
                } else if (oferta instanceof Formacion) {
                    Formacion formacion = (Formacion) oferta;
                    puedeModificar = puedeModificar || (formacion.getDocentes() != null && formacion.getDocentes().stream()
                            .anyMatch(docente -> docente.getDni().equals(dni)));
                }
                
                // Cargar módulos: todos para docentes/admin, solo visibles para alumnos
                List<Modulo> modulos;
                if (puedeModificar) {
                    System.out.println("🔓 Usuario con permisos de modificación - cargando TODOS los módulos");
                    modulos = moduloRepository.findByCursoOrderByFechaInicioModuloAsc(oferta);
                } else {
                    System.out.println("👤 Usuario alumno - cargando SOLO módulos visibles");
                    modulos = moduloRepository.findByCursoAndVisibilidadTrueOrderByFechaInicioModuloAsc(oferta);
                }
                
                System.out.println("📦 Módulos encontrados: " + modulos.size());
                for (Modulo m : modulos) {
                    System.out.println("  - " + m.getNombre() + " (visible: " + m.getVisibilidad() + ")");
                }
                
                model.addAttribute("curso", oferta); // Mantener nombre "curso" para compatibilidad
                model.addAttribute("modulos", modulos);
                model.addAttribute("inscripcion", inscripcion);
                model.addAttribute("puedeModificar", puedeModificar);
                System.out.println("👤 Puede modificar: " + puedeModificar);
                
                System.out.println("✅ Redirigiendo a template: aula");
                return "aula"; 
                
            } else {
                // Para otros tipos de ofertas académicas
                System.out.println("ℹ️  No es un curso, redirigiendo a aula general");
                model.addAttribute("oferta", oferta);
                model.addAttribute("inscripcion", inscripcion);
                return "aula/general"; // Esto buscaría templates/aula/general.html
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error al acceder al aula: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al acceder al aula: " + e.getMessage());
            return "misOfertasAcademicas";
        }
    }
    
    @GetMapping("/perfil")
    public String perfilUsuario(Authentication authentication, Model model) {
        String username = authentication.getName();
        
        // Buscar usuario por DNI (que es el username en el login)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            
            // Si es alumno, cargar datos académicos
            if (usuario instanceof Alumno) {
                model.addAttribute("alumno", (Alumno) usuario);
            }
        }
        
        return "perfilAlumno";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            Authentication authentication,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
            @RequestParam(required = false) String genero,
            @RequestParam String correo,
            @RequestParam("numTelefono") String numTelefono,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            System.out.println("[PERFIL] Solicitud de actualización recibida");
            String username = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByDni(username);
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensaje", "Usuario no encontrado");
                redirectAttributes.addFlashAttribute("tipo", "error");
                return "redirect:/perfil";
            }

            Usuario usuario = usuarioOpt.get();

            // Actualizar campos permitidos (DNI NO se modifica)
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setFechaNacimiento(fechaNacimiento);
            if (genero != null && !genero.isBlank()) {
                try {
                    usuario.setGenero(TipoGenero.valueOf(genero.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    redirectAttributes.addFlashAttribute("mensaje", "Género inválido");
                    redirectAttributes.addFlashAttribute("tipo", "error");
                    return "redirect:/perfil";
                }
            }
            usuario.setCorreo(correo);
            usuario.setNumTelefono(numTelefono);

            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al actualizar el perfil: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "error");
        }
        return "redirect:/perfil";
    }

    // Evitar 404 si el navegador hace GET a /perfil/actualizar (p.ej., refresh después del POST)
    @GetMapping("/perfil/actualizar")
    public String redirigirPerfilActualizar() {
        return "redirect:/perfil";
    }


}
