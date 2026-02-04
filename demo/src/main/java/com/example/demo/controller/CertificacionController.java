package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.enums.EstadoCertificacion;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.ActaCierreService;
import com.example.demo.service.CertificacionService;
import com.example.demo.service.CertificacionService.ResumenCertificaciones;

/**
 * Controller para gestionar el cierre de notas y certificaciones
 */
@Controller
@RequestMapping("/aula/oferta/{ofertaId}")
public class CertificacionController {
    
    @Autowired
    private CertificacionService certificacionService;
    
    @Autowired
    private ActaCierreService actaCierreService;

    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    @Autowired
    private CertificacionRepository certificacionRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private DocenteRepository docenteRepository;
    
    /**
     * Vista principal de certificaciones
     * GET /aula/oferta/{ofertaId}/certificaciones
     */
    @GetMapping("/certificaciones")
    public String verCertificaciones(
            @PathVariable Long ofertaId,
            @RequestParam(required = false) String filtro,
            Model model,
            Authentication auth) {
        
        // Obtener oferta
        OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
            .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
        
        // Verificar que el usuario sea docente de esta oferta
        String userDni = auth.getName();
        Usuario usuario = usuarioRepository.findByDni(userDni)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esDocenteDelCurso = false;
        if (usuario instanceof Docente) {
            Docente docente = (Docente) usuario;
            if (oferta instanceof Curso) {
                Curso curso = (Curso) oferta;
                esDocenteDelCurso = curso.getDocentes().contains(docente);
            } else if (oferta instanceof Formacion) {
                Formacion formacion = (Formacion) oferta;
                esDocenteDelCurso = formacion.getDocentes().contains(docente);
            }
        }
        
        boolean esAdmin = usuario.getRoles().stream()
            .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getNombre()));
        
        if (!esDocenteDelCurso && !esAdmin) {
            throw new RuntimeException("No tienes permisos para acceder a esta sección");
        }
        
        // Obtener resumen de certificaciones
        ResumenCertificaciones resumen = certificacionService.obtenerResumenCertificaciones(ofertaId);
        
        // Obtener lista de certificaciones según filtro
        List<Certificacion> certificaciones;
        if (filtro != null) {
            switch (filtro) {
                case "propuestos":
                    certificaciones = certificacionRepository.findByOfertaAndEstado(
                        oferta, EstadoCertificacion.PROPUESTA);
                    break;
                case "aprobados":
                    certificaciones = certificacionRepository.findByOfertaAndEstado(
                        oferta, EstadoCertificacion.APROBADO_DOCENTE);
                    break;
                case "rechazados":
                    certificaciones = certificacionRepository.findByOfertaAndEstado(
                        oferta, EstadoCertificacion.RECHAZADO_DOCENTE);
                    break;
                case "no-cumple":
                    certificaciones = certificacionRepository.findByOfertaAndEstado(
                        oferta, EstadoCertificacion.NO_APLICA);
                    break;
                default:
                    certificaciones = certificacionRepository.findByOferta(oferta);
            }
        } else {
            certificaciones = certificacionRepository.findByOferta(oferta);
        }
        
        // Calcular totales para certificación
        long totalACertificar = certificaciones.stream()
            .filter(c -> c.getEstado() == EstadoCertificacion.PROPUESTA || 
                        c.getEstado() == EstadoCertificacion.APROBADO_DOCENTE)
            .count();
        
        // Agregar datos al modelo
        model.addAttribute("oferta", oferta);
        model.addAttribute("certificaciones", certificaciones);
        model.addAttribute("resumen", resumen);
        model.addAttribute("totalACertificar", totalACertificar);
        model.addAttribute("filtroActual", filtro);
        model.addAttribute("puedeEditar", oferta.getEstado() != EstadoOferta.CERRADA);
        
        return "aula/certificaciones";
    }
    
    /**
     * Aprobar alumno manualmente para certificación
     * POST /aula/oferta/{ofertaId}/certificaciones/aprobar/{inscripcionId}
     */
    @PostMapping("/certificaciones/aprobar/{inscripcionId}")
    public String aprobarManual(
            @PathVariable Long ofertaId,
            @PathVariable Long inscripcionId,
            @RequestParam(required = false) String observaciones,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validar que oferta no esté cerrada
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            if (oferta.getEstado() == EstadoOferta.CERRADA) {
                redirectAttributes.addFlashAttribute("error", 
                    "No se pueden modificar certificaciones de una oferta CERRADA");
                return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
            }
            
            // Obtener docente actual
            String userDni = auth.getName();
            Docente docente = docenteRepository.findByDni(userDni)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            // Aprobar
            String obs = (observaciones != null && !observaciones.trim().isEmpty()) 
                ? observaciones 
                : "Aprobado manualmente por el docente";
            certificacionService.aprobarManualmente(inscripcionId, docente, obs);
            
            redirectAttributes.addFlashAttribute("success", 
                "Alumno aprobado exitosamente para certificación");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al aprobar alumno: " + e.getMessage());
        }
        
        return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
    }
    
    /**
     * Rechazar alumno para certificación
     * POST /aula/oferta/{ofertaId}/certificaciones/rechazar/{certificacionId}
     */
    @PostMapping("/certificaciones/rechazar/{certificacionId}")
    public String rechazarAlumno(
            @PathVariable Long ofertaId,
            @PathVariable Long certificacionId,
            @RequestParam String observaciones,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validar que oferta no esté cerrada
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            if (oferta.getEstado() == EstadoOferta.CERRADA) {
                redirectAttributes.addFlashAttribute("error", 
                    "No se pueden modificar certificaciones de una oferta CERRADA");
                return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
            }
            
            // Validar que se proporcionen observaciones
            if (observaciones == null || observaciones.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Debes proporcionar una justificación para rechazar al alumno");
                return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
            }
            
            // Obtener docente actual
            String userDni = auth.getName();
            Docente docente = docenteRepository.findByDni(userDni)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            // Rechazar
            certificacionService.rechazarManualmente(certificacionId, docente, observaciones);
            
            redirectAttributes.addFlashAttribute("success", 
                "Alumno rechazado para certificación");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al rechazar alumno: " + e.getMessage());
        }
        
        return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
    }
    
    /**
     * ACCIÓN CRÍTICA: Cerrar notas y generar acta de cierre
     * POST /aula/oferta/{ofertaId}/certificaciones/cerrar
     */
    @PostMapping("/certificaciones/cerrar")
    public String cerrarNotas(
            @PathVariable Long ofertaId,
            @RequestParam(required = false) String confirmacion,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validar confirmación
            if (!"CONFIRMAR".equals(confirmacion)) {
                redirectAttributes.addFlashAttribute("error", 
                    "Debes confirmar la acción para cerrar las notas");
                return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
            }
            
            // Obtener docente actual
            String userDni = auth.getName();
            Docente docente = docenteRepository.findByDni(userDni)
                .orElseThrow(() -> new RuntimeException("Docente no encontrado"));
            
            // Cerrar notas y emitir acta
            CertificacionService.CierreNotasResult resultado = 
                certificacionService.cerrarNotasYEmitirCertificados(ofertaId, docente);
            
            if (resultado.errores > 0) {
                redirectAttributes.addFlashAttribute("warning", 
                    String.format("Notas cerradas con %d aprobados. Hubo %d errores. Revisa los detalles.",
                        resultado.certificadosEmitidos, resultado.errores));
            } else {
                redirectAttributes.addFlashAttribute("success", 
                    String.format("¡Notas cerradas exitosamente! Se registraron %d alumnos aprobados. " +
                        "El acta de cierre está disponible para descarga.",
                        resultado.certificadosEmitidos));
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al cerrar notas: " + e.getMessage());
        }
        
        return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
    }
    
    /**
     * Descargar acta de cierre en PDF
     * GET /aula/oferta/{ofertaId}/certificaciones/acta-pdf
     */
    @GetMapping("/certificaciones/acta-pdf")
    public ResponseEntity<byte[]> descargarActaPdf(
            @PathVariable Long ofertaId,
            Authentication auth) {
        
        try {
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            if (oferta.getEstado() != EstadoOferta.CERRADA) {
                return ResponseEntity.badRequest()
                    .body("La oferta debe estar CERRADA para descargar el acta".getBytes());
            }
            
            // Generar PDF del acta
            byte[] pdfBytes = actaCierreService.generarActaCierrePDF(oferta);
            
            // Configurar respuesta HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            
            // Nombre del archivo
            String filename = String.format("Acta_Cierre_%s_%s.pdf",
                oferta.getNombre().replaceAll("[^a-zA-Z0-9]", "_"),
                java.time.LocalDate.now().toString()
            );
            headers.setContentDispositionFormData("attachment", filename);
            headers.add("X-Content-Type-Options", "nosniff");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
            
        } catch (Exception e) {
            System.err.println("❌ Error al generar acta PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(("Error al generar PDF: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exportar listado de alumnos a certificar (propuestos o aprobados manualmente)
     */
    @GetMapping("/certificaciones/exportar-lista")
    public ResponseEntity<byte[]> exportarListadoCertificar(
            @PathVariable Long ofertaId,
            Authentication auth) {
        try {
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

            byte[] pdfBytes = actaCierreService.generarListadoCertificarPDF(oferta);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = String.format("Listado_Certificar_%s_%s.pdf",
                oferta.getNombre().replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDate.now().toString());
            headers.setContentDispositionFormData("attachment", filename);
            headers.add("X-Content-Type-Options", "nosniff");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            System.err.println("❌ Error al exportar listado a certificar: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(("Error al generar PDF: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * Recalcular certificaciones (útil si se modificaron calificaciones)
     * POST /aula/oferta/{ofertaId}/certificaciones/recalcular
     */
    @PostMapping("/certificaciones/recalcular")
    public String recalcularCertificaciones(
            @PathVariable Long ofertaId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        
        try {
            OfertaAcademica oferta = ofertaRepository.findById(ofertaId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));
            
            if (oferta.getEstado() == EstadoOferta.CERRADA) {
                redirectAttributes.addFlashAttribute("error", 
                    "No se pueden recalcular certificaciones de una oferta CERRADA");
                return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
            }
            
            // Recalcular
            certificacionService.calcularCertificacionesAutomaticas(oferta);
            
            redirectAttributes.addFlashAttribute("success", 
                "Certificaciones recalculadas exitosamente");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al recalcular: " + e.getMessage());
        }
        
        return "redirect:/aula/oferta/" + ofertaId + "/certificaciones";
    }
}
