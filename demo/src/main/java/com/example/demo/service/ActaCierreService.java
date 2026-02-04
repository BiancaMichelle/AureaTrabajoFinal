package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.enums.EstadoCertificacion;
import com.example.demo.model.*;
import com.example.demo.repository.CertificacionRepository;

/**
 * Servicio para generar PDFs de actas de cierre
 * FUTURA IMPLEMENTACIÓN: Generar PDF profesional con iText o similar
 */
@Service
public class ActaCierreService {
    
    @Autowired
    private CertificacionRepository certificacionRepository;
    
    /**
     * Genera el acta de cierre en formato PDF
     * 
     * ESTRUCTURA DEL ACTA:
     * 1. Encabezado institucional
     * 2. Título: "ACTA DE CIERRE DE CALIFICACIONES"
     * 3. Datos de la oferta (nombre, fechas, docentes)
     * 4. Tabla de alumnos aprobados (DNI, Apellido, Nombre, Promedio, Asistencia)
     * 5. Firmas (Docente/Coordinador, Sello institucional)
     * 
     * @param oferta La oferta académica cerrada
     * @return Array de bytes del PDF generado
     */
    public byte[] generarActaCierrePDF(OfertaAcademica oferta) {
        try {
            System.out.println("📄 Generando acta de cierre para: " + oferta.getNombre());
            
            // Obtener aprobados
            List<Certificacion> aprobados = certificacionRepository.findByOferta(oferta).stream()
                .filter(c -> c.getEstado() == EstadoCertificacion.CERTIFICADO_EMITIDO)
                .sorted((c1, c2) -> {
                    String apellido1 = c1.getInscripcion().getAlumno().getApellido();
                    String apellido2 = c2.getInscripcion().getAlumno().getApellido();
                    return apellido1.compareTo(apellido2);
                })
                .collect(Collectors.toList());
            
            return generarActaPdfReal(oferta, aprobados);
            
        } catch (Exception e) {
            System.err.println("❌ Error al generar acta PDF: " + e.getMessage());
            throw new RuntimeException("Error al generar acta de cierre", e);
        }
    }

    /**
     * Genera un listado en PDF (placeholder texto plano) de alumnos a certificar
     * (propuestos o aprobados manualmente)
     */
    public byte[] generarListadoCertificarPDF(OfertaAcademica oferta) {
        try {
            System.out.println("📄 Generando listado de alumnos a certificar para: " + oferta.getNombre());

            List<Certificacion> aCertificar = certificacionRepository.findByOferta(oferta).stream()
                .filter(c -> c.getEstado() == EstadoCertificacion.PROPUESTA || c.getEstado() == EstadoCertificacion.APROBADO_DOCENTE)
                .sorted((c1, c2) -> c1.getInscripcion().getAlumno().getApellido()
                    .compareToIgnoreCase(c2.getInscripcion().getAlumno().getApellido()))
                .collect(Collectors.toList());

            return generarListadoCertificarPdfReal(oferta, aCertificar);
        } catch (Exception e) {
            System.err.println("❌ Error al generar listado a certificar: " + e.getMessage());
            throw new RuntimeException("Error al generar listado a certificar", e);
        }
    }
    
    private byte[] generarActaPdfReal(OfertaAcademica oferta, List<Certificacion> aprobados) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Encabezado
        document.add(new Paragraph("INSTITUTO DE EDUCACIÓN AUREA", headerFont));
        document.add(new Paragraph("\nACTA DE CIERRE DE CALIFICACIONES", titleFont));
        document.add(new Paragraph("\n"));

        // Datos de la oferta
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String tipoOferta = (oferta instanceof Curso) ? "CURSO" :
                           (oferta instanceof Formacion) ? "FORMACIÓN" : "SEMINARIO";

        document.add(new Paragraph("Tipo: " + tipoOferta, textFont));
        document.add(new Paragraph("Nombre: " + oferta.getNombre(), textFont));
        document.add(new Paragraph("Descripción: " + (oferta.getDescripcion() != null ? oferta.getDescripcion() : "N/A"), textFont));
        if (oferta.getFechaInicio() != null) {
            document.add(new Paragraph("Fecha Inicio: " + oferta.getFechaInicio().format(formatter), textFont));
        }
        if (oferta.getFechaFin() != null) {
            document.add(new Paragraph("Fecha Fin: " + oferta.getFechaFin().format(formatter), textFont));
        }
        document.add(new Paragraph("Fecha de Cierre de Calificaciones: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textFont));
        document.add(new Paragraph("\n"));

        // Docentes
        document.add(new Paragraph("Docentes a cargo:", headerFont));
        if (oferta instanceof Curso curso && curso.getDocentes() != null && !curso.getDocentes().isEmpty()) {
            for (Docente doc : curso.getDocentes()) {
                document.add(new Paragraph("• " + doc.getNombre() + " " + doc.getApellido() + " (DNI: " + doc.getDni() + ")", textFont));
            }
        } else if (oferta instanceof Formacion formacion && formacion.getDocentes() != null && !formacion.getDocentes().isEmpty()) {
            for (Docente doc : formacion.getDocentes()) {
                document.add(new Paragraph("• " + doc.getNombre() + " " + doc.getApellido() + " (DNI: " + doc.getDni() + ")", textFont));
            }
        } else {
            document.add(new Paragraph("(Sin docentes asignados)", textFont));
        }
        document.add(new Paragraph("\n"));

        // Tabla de aprobados
        document.add(new Paragraph("Nómina de alumnos aprobados", headerFont));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{2f, 3f, 3f, 2f, 2f});

        table.addCell(celda("DNI", true));
        table.addCell(celda("Apellido", true));
        table.addCell(celda("Nombre", true));
        table.addCell(celda("Promedio", true));
        table.addCell(celda("Asistencia", true));

        if (aprobados.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Sin alumnos aprobados", textFont));
            cell.setColspan(5);
            table.addCell(cell);
        } else {
            for (Certificacion cert : aprobados) {
                Usuario alumno = cert.getInscripcion().getAlumno();
                String promedio = cert.getPromedioGeneral() != null ? String.format("%.2f", cert.getPromedioGeneral()) : "N/A";
                String asistencia = cert.getPorcentajeAsistencia() != null ? String.format("%.1f%%", cert.getPorcentajeAsistencia()) : "N/A";

                table.addCell(celda(alumno.getDni(), false));
                table.addCell(celda(alumno.getApellido(), false));
                table.addCell(celda(alumno.getNombre(), false));
                table.addCell(celda(promedio, false));
                table.addCell(celda(asistencia, false));
            }
        }

        document.add(table);

        document.add(new Paragraph("\nTotal aprobados: " + aprobados.size(), textFont));

        document.add(new Paragraph("\n\nFirmas:", headerFont));
        document.add(new Paragraph("_________________________              _________________________", textFont));
        document.add(new Paragraph("    Firma Docente                         Firma Coordinador", textFont));

        document.close();
        return baos.toByteArray();
    }

    private byte[] generarListadoCertificarPdfReal(OfertaAcademica oferta, List<Certificacion> lista) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph("LISTADO DE ALUMNOS PROPUESTOS A CERTIFICAR", titleFont));
        document.add(new Paragraph("\n"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        document.add(new Paragraph("Oferta: " + oferta.getNombre(), textFont));
        if (oferta.getFechaInicio() != null) document.add(new Paragraph("Inicio: " + oferta.getFechaInicio().format(formatter), textFont));
        if (oferta.getFechaFin() != null) document.add(new Paragraph("Fin: " + oferta.getFechaFin().format(formatter), textFont));
        document.add(new Paragraph("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textFont));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setWidths(new float[]{2f, 3f, 3f, 2f, 2f, 2f});

        table.addCell(celda("DNI", true));
        table.addCell(celda("Apellido", true));
        table.addCell(celda("Nombre", true));
        table.addCell(celda("Prom", true));
        table.addCell(celda("Asist%", true));
        table.addCell(celda("Estado", true));

        if (lista.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase("Sin alumnos propuestos", textFont));
            cell.setColspan(6);
            table.addCell(cell);
        } else {
            for (Certificacion cert : lista) {
                Usuario alumno = cert.getInscripcion().getAlumno();
                String promedio = cert.getPromedioGeneral() != null ? String.format("%.2f", cert.getPromedioGeneral()) : "N/A";
                String asistencia = cert.getPorcentajeAsistencia() != null ? String.format("%.1f%%", cert.getPorcentajeAsistencia()) : "N/A";
                String estado = cert.getEstado() == EstadoCertificacion.APROBADO_DOCENTE ? "Aprob.Man" : "Propuesta";

                table.addCell(celda(alumno.getDni(), false));
                table.addCell(celda(alumno.getApellido(), false));
                table.addCell(celda(alumno.getNombre(), false));
                table.addCell(celda(promedio, false));
                table.addCell(celda(asistencia, false));
                table.addCell(celda(estado, false));
            }
        }

        document.add(table);
        document.add(new Paragraph("\nTotal a certificar: " + lista.size(), textFont));
        document.close();
        return baos.toByteArray();
    }
    
    private PdfPCell celda(String texto, boolean header) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 10, header ? Font.BOLD : Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setPadding(5);
        return cell;
    }
}
