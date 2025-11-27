package com.example.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.DTOMP.ReferenceRequest;
import com.example.demo.DTOMP.ResponseDTO;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.Inscripciones;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.MercadoPagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

/**
 * Controlador genérico para inscripciones a ofertas académicas.
 * Permite que tanto ALUMNOS como DOCENTES se inscriban como estudiantes.
 * Valida que un docente NO pueda inscribirse a una oferta donde ya enseña.
 */
@Controller
@RequestMapping("/inscribirse")
public class InscripcionController {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final OfertaAcademicaRepository ofertaAcademicaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final MercadoPagoService mercadoPagoService;
    private final com.example.demo.repository.PagoRepository pagoRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public InscripcionController(
            UsuarioRepository usuarioRepository,
            DocenteRepository docenteRepository,
            OfertaAcademicaRepository ofertaAcademicaRepository,
            InscripcionRepository inscripcionRepository,
            MercadoPagoService mercadoPagoService,
            com.example.demo.repository.PagoRepository pagoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.ofertaAcademicaRepository = ofertaAcademicaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.pagoRepository = pagoRepository;
    }

    @PostMapping("/{ofertaId}")
    public String inscribirseAOferta(@PathVariable Long ofertaId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String dni = authentication.getName();
            String rol = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority())
                    .orElse("");

            System.out.println(
                    "💰 Iniciando proceso de pago e inscripción para oferta: " + ofertaId + " (Rol: " + rol + ")");

            // Buscar el usuario (puede ser Alumno o Docente)
            Usuario usuario = usuarioRepository.findByDni(dni)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            System.out.println("👤 Usuario encontrado: " + usuario.getNombre() + " " + usuario.getApellido()
                    + " (Tipo: " + usuario.getClass().getSimpleName() + ")");

            // Buscar la oferta
            OfertaAcademica oferta = ofertaAcademicaRepository.findById(ofertaId)
                    .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            System.out.println("📚 Oferta encontrada: " + oferta.getNombre() + " (Tipo: "
                    + oferta.getClass().getSimpleName() + ")");

            // ========================================
            // VALIDACIÓN ESPECIAL PARA DOCENTES
            // ========================================
            if ("DOCENTE".equals(rol)) {
                Docente docente = docenteRepository.findByDni(dni)
                        .orElseThrow(() -> new RuntimeException("Docente no encontrado"));

                // Verificar si el docente ya enseña en esta oferta
                boolean yaEsDocenteAqui = false;

                if (oferta instanceof Curso) {
                    Curso curso = (Curso) oferta;
                    yaEsDocenteAqui = curso.getDocentes() != null && curso.getDocentes().stream()
                            .anyMatch(d -> d.getId().equals(docente.getId()));
                } else if (oferta instanceof Formacion) {
                    Formacion formacion = (Formacion) oferta;
                    yaEsDocenteAqui = formacion.getDocentes() != null && formacion.getDocentes().stream()
                            .anyMatch(d -> d.getId().equals(docente.getId()));
                }

                if (yaEsDocenteAqui) {
                    System.out.println("❌ El docente ya enseña en esta oferta");
                    redirectAttributes.addFlashAttribute("error",
                            "No puedes inscribirte como alumno a una oferta donde ya eres docente");
                    return "redirect:/publico";
                }

                System.out.println("✅ El docente NO enseña en esta oferta, puede inscribirse como alumno");
            }

            // Verificar si ya está inscrito
            List<Inscripciones> inscripcionesExistentes = inscripcionRepository.findByAlumnoDni(dni);
            boolean yaInscrito = inscripcionesExistentes.stream()
                    .anyMatch(ins -> ins.getOferta().getIdOferta().equals(ofertaId));

            if (yaInscrito) {
                redirectAttributes.addFlashAttribute("error", "Ya estás inscrito en esta oferta");
                return redirectSegunRol(rol);
            }

            // Verificar si tiene un pago pendiente para esta oferta
            boolean pagoPendiente = pagoRepository.existsByUsuarioAndOfertaAndEstadoPago(
                    usuario, oferta, com.example.demo.enums.EstadoPago.PENDIENTE);

            if (pagoPendiente) {
                redirectAttributes.addFlashAttribute("error",
                        "Ya tienes un pago pendiente para esta oferta. Por favor, revisa tu correo o intenta pagar nuevamente desde 'Mis Ofertas'.");
                return redirectSegunRol(rol);
            }

            // ========================================
            // VERIFICAR SI LA OFERTA TIENE COSTO
            // ========================================
            if (oferta.getCostoInscripcion() == null || oferta.getCostoInscripcion() <= 0) {
                // Inscripción gratuita - crear directamente
                System.out.println("✅ Oferta gratuita, creando inscripción directa");
                Inscripciones nuevaInscripcion = new Inscripciones();
                nuevaInscripcion.setAlumno(usuario);
                nuevaInscripcion.setOferta(oferta);
                nuevaInscripcion.setEstadoInscripcion(true);
                nuevaInscripcion.setFechaInscripcion(LocalDate.now());
                inscripcionRepository.save(nuevaInscripcion);

                redirectAttributes.addFlashAttribute("success",
                        "¡Te has inscrito exitosamente a " + oferta.getNombre() + "!");
                return redirectSegunRol(rol);
            }

            // ========================================
            // OFERTA CON COSTO - CREAR PREFERENCIA DE PAGO
            // ========================================
            System.out
                    .println("💵 Oferta con costo: $" + oferta.getCostoInscripcion() + ", creando preferencia de pago");

            // Crear el request para MercadoPago
            ReferenceRequest.ItemDTO item = new ReferenceRequest.ItemDTO(
                    oferta.getIdOferta().toString(),
                    oferta.getNombre(),
                    BigDecimal.valueOf(oferta.getCostoInscripcion()),
                    1);

            List<ReferenceRequest.ItemDTO> items = new ArrayList<>();
            items.add(item);
            // Crear el objeto PayerDTO
            ReferenceRequest.PayerDTO payer = new ReferenceRequest.PayerDTO(
                    usuario.getNombre() + " " + usuario.getApellido(),
                    usuario.getCorreo());

            // Crear el objeto BackUrlsDTO
            ReferenceRequest.BackUrlsDTO backUrls = new ReferenceRequest.BackUrlsDTO(
                    baseUrl + "/pago/success",
                    baseUrl + "/pago/failure",
                    baseUrl + "/pago/pending");

            // Crear el objeto ReferenceRequest
            ReferenceRequest request = new ReferenceRequest(
                    usuario.getId(),
                    BigDecimal.valueOf(oferta.getCostoInscripcion()),
                    payer,
                    backUrls,
                    items);

            // Crear preferencia con el usuario y oferta para generar el pago pendiente
            ResponseDTO response = mercadoPagoService.createPreference(request, usuario, oferta);

            System.out.println("✅ Preferencia creada: " + response.preferenceId());
            System.out.println("🔗 URL de pago: " + response.redirectUrl());

            // Redirigir al checkout de MercadoPago
            return "redirect:" + response.redirectUrl();

        } catch (MPException | MPApiException e) {
            System.err.println("❌ Error con MercadoPago: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al procesar el pago. Por favor, intenta nuevamente.");
            return "redirect:/publico";
        } catch (Exception e) {
            System.err.println("❌ Error al crear inscripción: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Hubo un error al procesar tu inscripción. Por favor, intenta nuevamente.");
            return "redirect:/publico";
        }
    }

    /**
     * Redirige al usuario según su rol después de la inscripción
     */
    private String redirectSegunRol(String rol) {
        if ("DOCENTE".equals(rol)) {
            return "redirect:/docente/mis-ofertas";
        } else {
            return "redirect:/alumno/mis-ofertas";
        }
    }
}
