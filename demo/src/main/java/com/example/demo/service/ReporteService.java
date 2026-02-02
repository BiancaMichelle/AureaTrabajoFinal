package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pago;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Usuario;
import com.example.demo.model.Cuota;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.enums.EstadoCuota;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;

import java.util.Map;
import java.util.Base64;
import java.awt.image.BufferedImage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private TemplateEngine templateEngine; // Thymeleaf Template Engine

    private String cargarEstilos() {
        try {
            ClassPathResource resource = new ClassPathResource("static/style/reporte_pdf.css");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "body { font-family: Helvetica; }"; // Fallback simple
        }
    }

    /**
     * Genera un PDF a partir de una plantilla HTML Thymeleaf
     */
    public ByteArrayInputStream generarReportePdfDesdePlantilla(String templateName, Map<String, Object> data) {
        log.info("📄 Iniciando generación de PDF desde plantilla: {}", templateName);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Context context = new Context();
            context.setVariables(data);
            
            log.debug("Procesando plantilla Thymeleaf...");
            String htmlContent = templateEngine.process(templateName, context);
            log.debug("HTML compilado (longitud: {})", htmlContent.length());
            
            // Validar HTML vacio
            if (htmlContent == null || htmlContent.isEmpty()) {
                throw new RuntimeException("La plantilla generó un HTML vacío");
            }

            log.debug("Renderizando PDF con OpenHTMLToPDF...");
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            
            byte[] pdfBytes = os.toByteArray();
            log.info("✅ PDF generado exitosamente. Tamaño: {} bytes", pdfBytes.length);
            
            return new ByteArrayInputStream(pdfBytes);
        } catch (Exception e) {
            log.error("❌ Error FATAL al generar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF desde plantilla HTML: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un gráfico de torta (Pie Chart) y lo devuelve como String Base64 para embeber en HTML
     */
    public String generarGraficoTortaBase64(Map<String, Number> datasetValues, String titulo) {
        try {
            DefaultPieDataset dataset = new DefaultPieDataset();
            for (Map.Entry<String, Number> entry : datasetValues.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart = ChartFactory.createPieChart(
                titulo,   // chart title
                dataset,          // data
                true,             // include legend
                true,
                false);

            BufferedImage image = chart.createBufferedImage(600, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<OfertaAcademica> filtrarOfertas(String nombre, String estado, Long categoriaId, LocalDate fechaInicio, LocalDate fechaFin, List<String> tipos) {
        List<OfertaAcademica> todas = ofertaRepository.findAll();
        
        return todas.stream()
            .filter(o -> nombre == null || nombre.isEmpty() || o.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .filter(o -> estado == null || estado.isEmpty() || (o.getEstado() != null && o.getEstado().name().equalsIgnoreCase(estado)))
            .filter(o -> categoriaId == null || o.getCategorias().stream().anyMatch(c -> c.getIdCategoria().equals(categoriaId)))
            .filter(o -> fechaInicio == null || (o.getFechaInicio() != null && !o.getFechaInicio().isBefore(fechaInicio)))
            .filter(o -> fechaFin == null || (o.getFechaFin() != null && !o.getFechaFin().isAfter(fechaFin)))
            .filter(o -> tipos == null || tipos.isEmpty() || tipos.contains(o.getTipoOferta()))
            .collect(Collectors.toList());
    }

    public List<Usuario> filtrarUsuarios(String rol, Boolean estado, String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Usuario> todos = usuarioRepository.findAll();
        
        return todos.stream()
            .filter(u -> rol == null || rol.isEmpty() || u.getRoles().stream().anyMatch(r -> r.getNombre().equalsIgnoreCase(rol)))
            .filter(u -> estado == null || u.isEstado() == estado)
            .filter(u -> nombre == null || nombre.isEmpty() || 
                    (u.getNombre() + " " + u.getApellido()).toLowerCase().contains(nombre.toLowerCase()) ||
                    u.getCorreo().toLowerCase().contains(nombre.toLowerCase()))
            .filter(u -> fechaInicio == null || (u.getFechaRegistro() != null && !u.getFechaRegistro().toLocalDate().isBefore(fechaInicio)))
            .filter(u -> fechaFin == null || (u.getFechaRegistro() != null && !u.getFechaRegistro().toLocalDate().isAfter(fechaFin)))
            .collect(Collectors.toList());
    }

    public ByteArrayInputStream generarReporteExcel(List<OfertaAcademica> ofertas) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ofertas Académicas");

            // Header
            String[] columns = {"ID", "Nombre", "Tipo", "Estado", "Inicio", "Fin", "Costo", "Inscriptos", "Activos", "Abandono (%)", "Ingresos Estimados"};
            Row headerRow = sheet.createRow(0);
            
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowIdx = 1;
            for (OfertaAcademica oferta : ofertas) {
                Row row = sheet.createRow(rowIdx++);
                
                int totalInscriptos = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
                long activos = oferta.getInscripciones() != null ? oferta.getInscripciones().stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
                double abandono = totalInscriptos > 0 ? ((double)(totalInscriptos - activos) / totalInscriptos) * 100 : 0.0;
                double ingresos = totalInscriptos * (oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0);

                row.createCell(0).setCellValue(oferta.getIdOferta());
                row.createCell(1).setCellValue(oferta.getNombre());
                row.createCell(2).setCellValue(oferta.getTipoOferta());
                row.createCell(3).setCellValue(oferta.getEstado() != null ? oferta.getEstado().name() : "N/A");
                row.createCell(4).setCellValue(oferta.getFechaInicio() != null ? oferta.getFechaInicio().toString() : "");
                row.createCell(5).setCellValue(oferta.getFechaFin() != null ? oferta.getFechaFin().toString() : "");
                row.createCell(6).setCellValue(oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0);
                row.createCell(7).setCellValue(totalInscriptos);
                row.createCell(8).setCellValue(activos);
                row.createCell(9).setCellValue(String.format("%.2f", abandono));
                row.createCell(10).setCellValue(ingresos);
            }
            
            // Auto size columns
            for(int i=0; i<columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream generarReportePDF(List<OfertaAcademica> ofertas) {
        // Configuramos márgenes: Izq, Der, Arr, Abajo (para dar espacio a encabezado/pie)
        Document document = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // --- DEFINICIÓN DE EVENTOS DE PÁGINA (Encabezado y Pie) ---
            writer.setPageEvent(new PdfPageEventHelper() {
                PdfTemplate total;
                BaseFont bf;

                @Override
                public void onOpenDocument(PdfWriter writer, Document document) {
                    try {
                        bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                        total = writer.getDirectContent().createTemplate(30, 16);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    float width = document.getPageSize().getWidth();
                    float height = document.getPageSize().getHeight();
                    
                    // --- ENCABEZADO CONSISTENTE ---
                    cb.saveState();
                    cb.setLineWidth(1f);
                    cb.moveTo(40, height - 40);
                    cb.lineTo(width - 40, height - 40);
                    cb.stroke();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 14);
                    cb.setTextMatrix(40, height - 35);
                    cb.showText("Aurea System"); // Nombre del Sistema
                    
                    cb.setFontAndSize(bf, 9);
                    
                    // Obtener usuario actual de forma segura
                    String usuario = "Usuario: Sistema";
                    if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                        usuario = "Usuario: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                    }
                    
                    String fecha = "Fecha: " + LocalDate.now().toString();
                    
                    float lenUser = bf.getWidthPoint(usuario, 9);
                    float lenDate = bf.getWidthPoint(fecha, 9);
                    
                    cb.setTextMatrix(width - 40 - lenUser, height - 25);
                    cb.showText(usuario);
                    
                    cb.setTextMatrix(width - 40 - lenDate, height - 35);
                    cb.showText(fecha);
                    
                    cb.endText();
                    cb.restoreState();

                    // --- PIE DE PÁGINA CONSISTENTE ---
                    cb.saveState();
                    cb.setLineWidth(0.5f);
                    cb.moveTo(40, 30);
                    cb.lineTo(width - 40, 30);
                    cb.stroke();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 9);
                    
                    cb.setTextMatrix(40, 20);
                    cb.showText("Informe Confidencial - Uso Interno");
                    
                    String text = "Página " + writer.getPageNumber() + " de ";
                    float textSize = bf.getWidthPoint(text, 9);
                    float x = width / 2; // Centrado
                    float y = 20;

                    cb.setTextMatrix(x - textSize / 2, y);
                    cb.showText(text);
                    cb.endText();

                    cb.addTemplate(total, x - textSize / 2 + textSize, y);
                    
                    cb.restoreState();
                }

                @Override
                public void onCloseDocument(PdfWriter writer, Document document) {
                    total.beginText();
                    total.setFontAndSize(bf, 9);
                    total.setTextMatrix(0, 0);
                    total.showText(String.valueOf(writer.getPageNumber() - 1));
                    total.endText();
                }
            });

            document.open();

            // --- 1. TÍTULO CON JERARQUÍA VISUAL ---
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Ofertas Académicas y Estadísticas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);
            
            // Subtítulo descriptivo
            Paragraph subtitle = new Paragraph("Informe detallado sobre programas educativos, inscripciones y rendimiento.", FontFactory.getFont(FontFactory.HELVETICA, 11, java.awt.Color.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20f);
            document.add(subtitle);

            // --- 2. RESUMEN EJECUTIVO (Datos Relevantes) ---
            addSummarySection(document, ofertas);
            
            document.add(Chunk.NEWLINE);

            // --- 3. TABLA DE DETALLES (Claridad y Estructura) ---
            addOfertasTable(document, ofertas);

            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addSummarySection(Document doc, List<OfertaAcademica> ofertas) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10f);
        summaryTable.setSpacingAfter(20f);
        summaryTable.getDefaultCell().setBorder(0);
        
        // Cálculos estdísticos
        int totalOfertas = ofertas.size();
        long totalInscritos = ofertas.stream().mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0).sum();
        long activos = ofertas.stream().mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0).sum();
        double ingresos = ofertas.stream().mapToDouble(o -> {
             int n = o.getInscripciones() != null ? o.getInscripciones().size() : 0;
             return n * (o.getCostoInscripcion() != null ? o.getCostoInscripcion() : 0.0);
        }).sum();

        summaryTable.addCell(createKpiCell("Total Programas", String.valueOf(totalOfertas)));
        summaryTable.addCell(createKpiCell("Total Inscripciones", String.valueOf(totalInscritos)));
        summaryTable.addCell(createKpiCell("Alumnos Activos", String.valueOf(activos)));
        summaryTable.addCell(createKpiCell("Ingresos Estimados", String.format("$ %,.2f", ingresos)));

        doc.add(summaryTable);
    }
    
    private PdfPCell createKpiCell(String title, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(0);
        cell.setPadding(10f);
        cell.setBackgroundColor(new java.awt.Color(245, 247, 250)); // Gris muy claro
        
        Paragraph pTitle = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.DARK_GRAY));
        pTitle.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph pValue = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, java.awt.Color.BLACK));
        pValue.setAlignment(Element.ALIGN_CENTER);
        
        cell.addElement(pTitle);
        cell.addElement(pValue);
        return cell;
    }

    private void addOfertasTable(Document doc, List<OfertaAcademica> ofertas) throws DocumentException {
        // Configuración de tabla para legibilidad
        PdfPTable table = new PdfPTable(8); // Reducir columnas para mejor espacio
        table.setWidthPercentage(100);
        table.setWidths(new int[]{4, 2, 2, 2, 2, 1, 1, 2});
        table.setHeaderRows(1);

        String[] headers = {"Programa Educativo", "Tipo", "Estado", "Inicio", "Fin", "Insc.", "Act.", "Ingresos"};
        
        // Estilo Header
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
        
        for (String header : headers) {
            PdfPCell hcell = new PdfPCell(new Phrase(header, headerFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hcell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            hcell.setBackgroundColor(new java.awt.Color(74, 105, 189)); // Azul institucional
            hcell.setPadding(6f);
            hcell.setBorderWidth(0);
            table.addCell(hcell);
        }

        // Estilo Datos
        com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alternate = false;

        for (OfertaAcademica oferta : ofertas) {
            int totalInscriptos = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
            long activos = oferta.getInscripciones() != null ? oferta.getInscripciones().stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
            double ingresos = totalInscriptos * (oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0);
            
            java.awt.Color bgColor = alternate ? new java.awt.Color(240, 240, 240) : java.awt.Color.WHITE;

            addCell(table, oferta.getNombre(), dataFont, bgColor, Element.ALIGN_LEFT);
            addCell(table, oferta.getTipoOferta(), dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, oferta.getEstado() != null ? oferta.getEstado().name() : "-", dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, oferta.getFechaInicio() != null ? oferta.getFechaInicio().toString() : "", dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, oferta.getFechaFin() != null ? oferta.getFechaFin().toString() : "", dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(totalInscriptos), dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, String.valueOf(activos), dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, String.format("$ %,.0f", ingresos), dataFont, bgColor, Element.ALIGN_RIGHT);
            
            alternate = !alternate;
        }

        doc.add(table);
    }
    
    private void addCell(PdfPTable table, String text, com.lowagie.text.Font font, java.awt.Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        cell.setBorderColor(new java.awt.Color(220, 220, 220));
        table.addCell(cell);
    }

    public ByteArrayInputStream generarReporteEstadisticoPDF(List<OfertaAcademica> ofertas, LocalDate fechaInicio, LocalDate fechaFin) {
        Document document = new Document(PageSize.A4, 40, 40, 110, 60); // Increased top margin for larger header
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // --- EVENT HELPER (Header/Footer) ---
            writer.setPageEvent(new PdfPageEventHelper() {
                BaseFont bf = null;
                PdfTemplate total;

                @Override
                public void onOpenDocument(PdfWriter writer, Document document) {
                    total = writer.getDirectContent().createTemplate(30, 16);
                    try {
                        bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    cb.saveState();
                    
                    float width = document.getPageSize().getWidth();
                    float height = document.getPageSize().getHeight();
                    
                    // --- HEADER ---
                    float headerY = height - 35;
                    
                    // 1. Nombre del Sistema (Marca)
                    cb.beginText();
                    cb.setFontAndSize(bf, 16);
                    cb.setColorFill(new java.awt.Color(44, 62, 80));
                    cb.setTextMatrix(40, headerY);
                    cb.showText("Aurea System");
                    cb.endText();
                    
                    // 2. Nombre del Instituto
                    String nombreInstituto = "Instituto Académico";
                    if (!ofertas.isEmpty() && ofertas.get(0).getInstituto() != null) {
                        nombreInstituto = ofertas.get(0).getInstituto().getNombreInstituto();
                    }
                    cb.beginText();
                    cb.setFontAndSize(bf, 12);
                    cb.setColorFill(java.awt.Color.GRAY);
                    cb.setTextMatrix(40, headerY - 15);
                    cb.showText(nombreInstituto);
                    cb.endText();

                    // 3. Info Derecha (Fecha, Usuario)
                    cb.beginText();
                    cb.setFontAndSize(bf, 9);
                    cb.setColorFill(java.awt.Color.BLACK);
                    
                    String usuario = "Generado por: Sistema";
                    String rol = "";
                    if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                        String name = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                        usuario = "Generado por: " + name;
                        // Intentar obtener rol (simplificado)
                        rol = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();
                    }
                    
                    String fechaGen = "Generación: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    
                    float lenUser = bf.getWidthPoint(usuario, 9);
                    float lenDate = bf.getWidthPoint(fechaGen, 9);
                    
                    cb.setTextMatrix(width - 40 - lenDate, headerY);
                    cb.showText(fechaGen);
                    
                    cb.setTextMatrix(width - 40 - lenUser, headerY - 12);
                    cb.showText(usuario);
                    cb.endText();
                    
                    // 4. Tipo de Reporte y Período (Diferenciado)
                    cb.setLineWidth(1.5f);
                    cb.setColorStroke(new java.awt.Color(74, 105, 189)); // Brand Blue
                    cb.moveTo(40, headerY - 30);
                    cb.lineTo(width - 40, headerY - 30);
                    cb.stroke();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 14);
                    cb.setColorFill(new java.awt.Color(74, 105, 189));
                    cb.setTextMatrix(40, headerY - 50);
                    cb.showText("Reporte Estadístico del Sistema Académico");
                    cb.endText();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 10);
                    cb.setColorFill(java.awt.Color.DARK_GRAY);
                    String periodo = "Período Analizado: " + 
                        (fechaInicio != null ? fechaInicio.toString() : "Inicio Historico") + " al " +
                        (fechaFin != null ? fechaFin.toString() : "Actualidad");
                    cb.setTextMatrix(40, headerY - 65);
                    cb.showText(periodo);
                    cb.endText();

                    // Footer
                    cb.setLineWidth(0.5f);
                    cb.setColorStroke(java.awt.Color.GRAY);
                    cb.moveTo(40, 30);
                    cb.lineTo(width - 40, 30);
                    cb.stroke();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 8);
                    cb.setColorFill(java.awt.Color.GRAY);
                    cb.setTextMatrix(40, 20);
                    cb.showText("Aurea System - Informe Confidencial para Uso Gerencial");
                    
                    String text = "Página " + writer.getPageNumber() + " de ";
                    float textSize = bf.getWidthPoint(text, 8);
                    float x = width / 2;
                    float y = 20;

                    cb.setTextMatrix(x - textSize / 2, y);
                    cb.showText(text);
                    cb.endText();

                    cb.addTemplate(total, x - textSize / 2 + textSize, y);
                    cb.restoreState();
                }

                @Override
                public void onCloseDocument(PdfWriter writer, Document document) {
                    total.beginText();
                    total.setFontAndSize(bf, 8);
                    total.setTextMatrix(0, 0);
                    total.showText(String.valueOf(writer.getPageNumber() - 1));
                    total.endText();
                }
            });

            document.open();

            // Font Styles
            com.lowagie.text.Font h1Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(44, 62, 80));
            com.lowagie.text.Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new java.awt.Color(52, 73, 94));
            com.lowagie.text.Font pFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            // --- 1. RESUMEN EJECUTIVO ---
            document.add(new Paragraph("1. Resumen Ejecutivo", h1Font));
            document.add(Chunk.NEWLINE);
            addSummarySection(document, ofertas);
            document.add(Chunk.NEWLINE);

            // --- 2. ESTADÍSTICAS DE OFERTAS ACADÉMICAS ---
            document.add(new Paragraph("2. Estadísticas de Ofertas Académicas", h1Font));
            document.add(new Paragraph("Desglose de la oferta educativa por tipo, modalidad y estado.", pFont));
            document.add(Chunk.NEWLINE);
            
            // Cálculos
            long totalOfertas = ofertas.size();
            java.util.Map<String, Long> porEstado = ofertas.stream()
                .collect(Collectors.groupingBy(o -> o.getEstado() != null ? o.getEstado().name() : "DESCONOCIDO", Collectors.counting()));
            java.util.Map<String, Long> porModalidad = ofertas.stream()
                .collect(Collectors.groupingBy(o -> o.getModalidad() != null ? o.getModalidad().name() : "SIN DEFINIR", Collectors.counting()));
            java.util.Map<String, Long> porTipo = ofertas.stream()
                .collect(Collectors.groupingBy(OfertaAcademica::getTipoOferta, Collectors.counting()));
            
            // Gráficos (Tablas visuales)
            addChartSection(document, "Ofertas por Tipo", porTipo, new java.awt.Color(230, 126, 34));
            document.add(Chunk.NEWLINE);
            addChartSection(document, "Ofertas por Estado", porEstado, new java.awt.Color(46, 204, 113));
            document.add(Chunk.NEWLINE);
            addChartSection(document, "Ofertas por Modalidad", porModalidad, new java.awt.Color(52, 152, 219));
            document.add(Chunk.NEWLINE);

            // --- 3. ESTADÍSTICAS DE ALUMNOS (Base de Datos) ---
            document.add(new Paragraph("3. Estadísticas de Alumnos", h1Font));
            document.add(Chunk.NEWLINE);
            
            // Obtener alumnos desde BD por Rol
            List<Usuario> todosAlumnos = usuarioRepository.findByRolesNombre("ALUMNO");
            long totalAlumnos = todosAlumnos.size();
            long alumnosAlta = todosAlumnos.stream().filter(Usuario::isEstado).count();
            long alumnosBaja = totalAlumnos - alumnosAlta;
            
            // Datos Operativos (Inscripciones)
            long totalInscripciones = ofertas.stream()
                 .mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0).sum();
            
            // Tabla KPIs Alumnos
            PdfPTable studentsTable = new PdfPTable(3);
            studentsTable.setWidthPercentage(100);
            studentsTable.addCell(createKpiCell("Total Registrados", String.valueOf(totalAlumnos)));
            studentsTable.addCell(createKpiCell("Alumnos de Alta", String.valueOf(alumnosAlta)));
            studentsTable.addCell(createKpiCell("Alumnos de Baja", String.valueOf(alumnosBaja)));
            document.add(studentsTable);
            
            document.add(Chunk.NEWLINE);

            // --- 4. ESTADÍSTICAS DE DOCENTES (Base de Datos) ---
            document.add(new Paragraph("4. Estadísticas de Docentes", h1Font));
            document.add(Chunk.NEWLINE);
            
            // Obtener docentes desde BD por Rol
            List<Usuario> todosDocentes = usuarioRepository.findByRolesNombre("DOCENTE");
            long totalDocentes = todosDocentes.size();
            long docentesAlta = todosDocentes.stream().filter(Usuario::isEstado).count();
            long docentesBaja = totalDocentes - docentesAlta;
            
            // Docentes con asignación actual (filtrando de las ofertas)
            java.util.Set<java.util.UUID> docentesAsignadosIds = new java.util.HashSet<>();
             for (OfertaAcademica o : ofertas) {
                if (o instanceof Curso) {
                    Curso c = (Curso) o;
                    if (c.getDocentes() != null) {
                        for (Docente d : c.getDocentes()) {
                             docentesAsignadosIds.add(d.getId());
                        }
                    }
                }
            }
            long docentesConAsignacion = docentesAsignadosIds.size();
            long docentesSinAsignacion = docentesAlta - docentesConAsignacion;
            if (docentesSinAsignacion < 0) docentesSinAsignacion = 0; // Prevent negative if data inconsistency

            // Tabla KPIs Docentes
            PdfPTable teachersTable = new PdfPTable(3);
            teachersTable.setWidthPercentage(100);
            teachersTable.addCell(createKpiCell("Total Docentes", String.valueOf(totalDocentes)));
            teachersTable.addCell(createKpiCell("Docentes de Alta", String.valueOf(docentesAlta)));
            teachersTable.addCell(createKpiCell("Docentes de Baja", String.valueOf(docentesBaja)));
            document.add(teachersTable);
            
            document.add(Chunk.NEWLINE);
            
            // --- GRÁFICO COMPARATIVO: Población Institucional ---
            
            // Obtener admins desde BD
            List<Usuario> todosAdmins = usuarioRepository.findByRolesNombre("ADMIN");
            long adminsAlta = todosAdmins.stream().filter(Usuario::isEstado).count();
            
            // Total Usuarios Sistema
            long totalUsuariosSistema = usuarioRepository.count();

            java.util.Map<String, Long> poblacionData = new java.util.LinkedHashMap<>();
            poblacionData.put("Alumnos de Alta", alumnosAlta);
            poblacionData.put("Docentes de Alta", docentesAlta);
            poblacionData.put("Admins de Alta", adminsAlta);
            poblacionData.put("Total Usuarios", totalUsuariosSistema);
            
            addChartSection(document, "Comparativa Población Institucional (Altas vs Total)", poblacionData, new java.awt.Color(52, 73, 94));
            document.add(Chunk.NEWLINE);
            
            // --- 5. INDICADORES CLAVE (KPIs Académicos) ---
            document.add(new Paragraph("5. Indicadores Clave (KPIs Académicos)", h1Font));
            document.add(Chunk.NEWLINE);
            
            // Tasa Ocupacion
            long totalCupos = ofertas.stream().mapToLong(o -> o.getCupos() != null && o.getCupos() < 10000 ? o.getCupos() : 0).sum();
            // Ignoramos cupos infinitos para el calculo de tasa
            long inscritosEnOfertasConCupo = ofertas.stream()
                .filter(o -> o.getCupos() != null && o.getCupos() < 10000)
                .mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0).sum();
                
            double tasaOcupacion = totalCupos > 0 ? (double) inscritosEnOfertasConCupo / totalCupos * 100 : 0;
            
            // Ofertas Baja Demanda (< 5 alumnos)
            long bajaDemanda = ofertas.stream()
                .filter(o -> o.getInscripciones() != null && o.getInscripciones().size() < 5)
                .count();

            PdfPTable kpiTable = new PdfPTable(2);
            kpiTable.setWidthPercentage(100);
            kpiTable.addCell(createKpiCell("Tasa de Ocupación Global", String.format("%.1f %%", tasaOcupacion)));
            kpiTable.addCell(createKpiCell("Ofertas Baja Demanda (<5)", String.valueOf(bajaDemanda)));
            document.add(kpiTable);
            
            document.add(Chunk.NEWLINE);
            
            // --- 6. ANÁLISIS COMPARATIVO ---
            document.add(new Paragraph("6. Observaciones y Análisis", h1Font));
            Paragraph obs = new Paragraph(
                "Este reporte refleja la situación actual de los programas académicos. " + 
                "Se recomienda revisar las ofertas con baja demanda para optimizar la asignación de docentes. " +
                "La tasa de ocupación del " + String.format("%.1f%%", tasaOcupacion) + " indica " +
                (tasaOcupacion > 70 ? "un alto rendimiento de las aulas." : "oportunidades de mejora en la captación."),
                pFont
            );
            obs.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(obs);

            document.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addChartSection(Document doc, String chartTitle, java.util.Map<String, Long> data, java.awt.Color barColor) throws DocumentException {
        PdfPTable container = new PdfPTable(1);
        container.setWidthPercentage(100);
        container.getDefaultCell().setBorder(0);
        
        // Header del Gráfico
        PdfPCell headerCell = new PdfPCell(new Phrase(chartTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        headerCell.setBorder(0);
        headerCell.setPaddingBottom(10f);
        container.addCell(headerCell);
        
        doc.add(container);

        // Tabla de barras
        PdfPTable chartTable = new PdfPTable(3); // Label | Bar | Value
        chartTable.setWidthPercentage(100);
        try {
            chartTable.setWidths(new int[]{30, 60, 10});
        } catch(DocumentException e) {}
        
        long maxValue = data.values().stream().mapToLong(v -> v).max().orElse(1);
        
        for (java.util.Map.Entry<String, Long> entry : data.entrySet()) {
            // Label
            PdfPCell labelCell = new PdfPCell(new Phrase(entry.getKey(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            labelCell.setBorder(0);
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            labelCell.setPaddingRight(10f);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            chartTable.addCell(labelCell);
            
            // Bar Calculation
            float percentage = (float) entry.getValue() / maxValue * 100;
            if (percentage < 1 && entry.getValue() > 0) percentage = 1; 
            
            PdfPTable barContainer = new PdfPTable(2); // Colored | Empty
            barContainer.setWidthPercentage(100);
            float[] widths = {percentage, 100 - percentage};
            try {
                if (percentage >= 100) {
                     widths = new float[]{100f, 0f};
                }
                // Fix for 0%
                if (percentage <= 0) {
                    widths = new float[]{0f, 100f};
                }
                barContainer.setWidths(widths);
            } catch(Exception e) {}
            
            PdfPCell barCell = new PdfPCell();
            barCell.setBackgroundColor(entry.getValue() > 0 ? barColor : java.awt.Color.LIGHT_GRAY);
            barCell.setBorder(0);
            barCell.setFixedHeight(12f);
            barContainer.addCell(barCell);
            
            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(0);
            barContainer.addCell(emptyCell);
            
            PdfPCell barWrapper = new PdfPCell(barContainer);
            barWrapper.setBorder(0);
            barWrapper.setVerticalAlignment(Element.ALIGN_MIDDLE);
            barWrapper.setPaddingTop(5f);
            barWrapper.setPaddingBottom(5f);
            chartTable.addCell(barWrapper);
            
            // Value
            PdfPCell valueCell = new PdfPCell(new Phrase(entry.getValue().toString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            valueCell.setBorder(0);
            valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            valueCell.setPaddingLeft(5f);
            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            chartTable.addCell(valueCell);
        }
        
        doc.add(chartTable);
    }

    public ByteArrayInputStream generarComprobantePagoPDF(Pago pago) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Estilos
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            com.lowagie.text.Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Título
            Paragraph title = new Paragraph("AUREA - Comprobante de Pago", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Datos del Alumno
            document.add(new Paragraph("Datos del Alumno:", headerFont));
            document.add(new Paragraph("Nombre: " + (pago.getUsuario() != null ? pago.getUsuario().getNombre() + " " + pago.getUsuario().getApellido() : "N/A"), normalFont));
            document.add(new Paragraph("DNI: " + (pago.getUsuario() != null ? pago.getUsuario().getDni() : "N/A"), normalFont));
            document.add(new Paragraph("Email: " + (pago.getEmailPagador() != null ? pago.getEmailPagador() : "N/A"), normalFont));
            document.add(Chunk.NEWLINE);

            // Datos del Pago
            document.add(new Paragraph("Detalles de la Transacción:", headerFont));
            
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addTableRow(table, "Concepto:", pago.getDescripcion() != null ? pago.getDescripcion() : "Inscripción");
            addTableRow(table, "Oferta Académica:", pago.getOferta() != null ? pago.getOferta().getNombre() : "N/A");
            addTableRow(table, "Referencia de Pago:", pago.getExternalReference() != null ? pago.getExternalReference() : "N/A");
            addTableRow(table, "ID Transacción (MP):", pago.getPaymentId() != null ? pago.getPaymentId().toString() : "N/A");
            addTableRow(table, "Método de Pago:", pago.getTipoPago() != null ? pago.getTipoPago() : "N/A");
            addTableRow(table, "Fecha:", pago.getFechaAprobacion() != null ? pago.getFechaAprobacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : LocalDate.now().toString());
            
            // Monto destacado
            PdfPCell cellLabel = new PdfPCell(new Phrase("Monto Pagado:", headerFont));
            cellLabel.setBorder(0);
            cellLabel.setPadding(5);
            table.addCell(cellLabel);

            PdfPCell cellValue = new PdfPCell(new Phrase("$ " + (pago.getMonto() != null ? pago.getMonto().toString() : "0.00"), titleFont));
            cellValue.setBorder(0);
            cellValue.setPadding(5);
            table.addCell(cellValue);

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Pie de página
            Paragraph footer = new Paragraph("Este documento es un comprobante válido de pago emitido por Aurea.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Versión HTML+CSS (Profesional) del reporte de pagos
     */
    public ByteArrayInputStream generarReportePagosPDF(List<Pago> pagos, Long filtroCursoId) {
        // Calcular estadísticas
        double totalMonto = pagos.stream().mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0).sum();
        
        // --- 1. Calcular Morosidad (Cuotas vencidas) ---
        LocalDate today = LocalDate.now();
        List<com.example.demo.model.Cuota> allCuotas = cuotaRepository.findAll();
        
        List<com.example.demo.model.Cuota> cuotasVencidas = allCuotas.stream()
            .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE)
            .filter(c -> c.getFechaVencimiento() != null && c.getFechaVencimiento().isBefore(today))
            .filter(c -> filtroCursoId == null || (c.getInscripcion() != null && c.getInscripcion().getOferta() != null && c.getInscripcion().getOferta().getIdOferta().equals(filtroCursoId)))
            .collect(Collectors.toList());

        long morososCount = cuotasVencidas.stream()
            .map(c -> c.getInscripcion().getAlumno().getId())
            .distinct()
            .count();
        
        double deudaTotal = cuotasVencidas.stream()
             .mapToDouble(c -> c.getMonto() != null ? c.getMonto().doubleValue() : 0)
             .sum();

        // --- 2. Ingresos por Curso (Breakdown) ---
        Map<String, Double> ingresosPorCurso = pagos.stream()
             .filter(p -> p.getOferta() != null)
             .collect(Collectors.groupingBy(
                 p -> p.getOferta().getNombre(),
                 Collectors.summingDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
             ));

        // --- 3. Cuotas Pendientes / Por vencer (Detalle) ---
        List<Map<String, Object>> detalleCuotasList = cuotasVencidas.stream()
               .limit(100) // Limitamos para no romper el PDF si son muchas
               .map(c -> {
                   Map<String, Object> map = new java.util.HashMap<>();
                   map.put("alumno", c.getInscripcion().getAlumno().getNombre() + " " + c.getInscripcion().getAlumno().getApellido());
                   map.put("curso", c.getInscripcion().getOferta() != null ? c.getInscripcion().getOferta().getNombre() : "N/A");
                   map.put("cuota", "C-" + c.getNumeroCuota());
                   map.put("vencimiento", c.getFechaVencimiento() != null ? c.getFechaVencimiento().toString() : "-");
                   map.put("monto", String.format("$ %.2f", c.getMonto() != null ? c.getMonto().doubleValue() : 0.0));
                   // Calcular interés simple del 5% como regla de negocio de ejemplo para el reporte
                   double interes = (c.getMonto() != null ? c.getMonto().doubleValue() * 0.05 : 0.0);
                   map.put("interes", String.format("$ %.2f", interes));
                   return map;
               }).collect(Collectors.toList());

        // Preparar datos para el gráfico
        Map<String, Number> datosGrafico = pagos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstadoPago().toString(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String chartBase64 = generarGraficoTortaBase64(datosGrafico, "Distribución por Estado");

        // Preparar contexto Thymeleaf
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("pagos", pagos);
        data.put("fechaEmision", LocalDate.now().toString());
        data.put("reporteId", "RP-" + System.currentTimeMillis());
        data.put("totalRecaudado", String.format("$ %.2f", totalMonto));
        data.put("cantidadTransacciones", pagos.size());
        data.put("morososCount", morososCount);
        data.put("deudaTotal", String.format("$ %.2f", deudaTotal));
        data.put("ingresosPorCurso", ingresosPorCurso);
        data.put("detalleCuotas", detalleCuotasList); // Pasamos la nueva lista al contexto
        data.put("chartImage", chartBase64);
        data.put("estilos", cargarEstilos());

        return generarReportePdfDesdePlantilla("reporte/reportePagos", data);
    }

    /**
     * Versión HTML+CSS (Profesional) del reporte de ofertas
     */
    public ByteArrayInputStream generarReporteOfertasPDF(List<OfertaAcademica> ofertas, LocalDate fechaInicio, LocalDate fechaFin) {
        // --- KPIs ---
        long totalOfertas = ofertas.size();
        long ofertasPublicadas = ofertas.stream().filter(o -> o.getEstado() != null && "PUBLICADO".equals(o.getEstado().name())).count();
        long totalInscritos = ofertas.stream().mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0).sum();
        
        // Ingresos Estimados (Inscritos * Costo)
        double ingresosEstimados = ofertas.stream()
                .mapToDouble(o -> {
                    long insc = o.getInscripciones() != null ? o.getInscripciones().size() : 0;
                    double cost = o.getCostoInscripcion() != null ? o.getCostoInscripcion() : 0.0;
                    return insc * cost;
                }).sum();

        // Calculo de ocupación y alertas (Ofertas con cupos definidos)
        long ofertasConBajaOcupacion = ofertas.stream()
            .filter(o -> {
                if(o.getCupos() == null || o.getCupos() >= 2000000000) return false;
                long insc = o.getInscripciones() != null ? o.getInscripciones().size() : 0;
                double porcentaje = (double) insc / o.getCupos();
                return porcentaje < 0.5; // Menos del 50%
            }).count();

        long ofertasLlenas = ofertas.stream()
            .filter(o -> {
                if(o.getCupos() == null || o.getCupos() >= 2000000000) return false;
                long insc = o.getInscripciones() != null ? o.getInscripciones().size() : 0;
                return insc >= o.getCupos();
            }).count();

        // --- Gráfico: Ofertas por Tipo ---
        Map<String, Number> datosGrafico = ofertas.stream()
            .collect(Collectors.groupingBy(
                o -> o.getTipoOferta() != null ? o.getTipoOferta() : "DESCONOCIDO", 
                Collectors.counting()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String chartBase64 = generarGraficoTortaBase64(datosGrafico, "Distribución por Tipo");

        // --- Contexto ---
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        // Periodo Text
        String periodoTexto = "Histórico Completo";
        if (fechaInicio != null && fechaFin != null) {
            periodoTexto = fechaInicio.format(formatter) + " al " + fechaFin.format(formatter);
        } else if (fechaInicio != null) {
            periodoTexto = "Desde " + fechaInicio.format(formatter);
        } else if (fechaFin != null) {
            periodoTexto = "Hasta " + fechaFin.format(formatter);
        }
        
        // Info usuario generador
        String generadoPor = "Sistema";
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
             generadoPor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("ofertas", ofertas);
        data.put("fechaEmision", LocalDate.now().format(formatter));
        data.put("periodoReporte", periodoTexto);
        data.put("generadoPor", generadoPor);
        data.put("reporteId", "RO-" + System.currentTimeMillis());
        data.put("totalOfertas", totalOfertas);
        data.put("ofertasPublicadas", ofertasPublicadas);
        data.put("totalInscritos", totalInscritos);
        data.put("ofertasBajaOcupacion", ofertasConBajaOcupacion);
        data.put("ofertasLlenas", ofertasLlenas);
        data.put("ingresosEstimados", String.format("$ %.2f", ingresosEstimados));
        data.put("chartImage", chartBase64);
        data.put("estilos", cargarEstilos());

        return generarReportePdfDesdePlantilla("reporte/reporteOfertas", data);
    }

    /**
     * Versión HTML+CSS (Profesional) del reporte de usuarios
     */
    @Transactional(readOnly = true)
    public ByteArrayInputStream generarReporteUsuariosPDF(List<Usuario> usuarios, LocalDate fechaInicio, LocalDate fechaFin) {
        // --- KPIs ---
        long totalUsuariosReporte = usuarios.size();
        long totalUsuariosSistema = usuarioRepository.count();
        long usuariosActivos = usuarios.stream().filter(Usuario::isEstado).count();
        long usuariosInactivos = totalUsuariosReporte - usuariosActivos;

        // Nuevos en el periodo (solo si hay fechaInicio)
        long nuevosIngresos = 0;
        if(fechaInicio != null) {
             nuevosIngresos = usuarios.stream().filter(u -> u.getFechaRegistro() != null && !u.getFechaRegistro().toLocalDate().isBefore(fechaInicio)).count();
        } else {
             // Si no hay filtro fecha inicio, mostramos total como nuevos (o 0 según semántica)
             nuevosIngresos = totalUsuariosReporte; 
        }
        
        // --- Distribución por Rol ---
        // Asumiendo que un usuario puede tener varios roles, contamos ocurrencias de roles principales
        Map<String, Long> rolesCount = usuarios.stream()
            .flatMap(u -> u.getRoles().stream())
            .collect(Collectors.groupingBy(
                r -> r.getNombre(), // Nombre del rol
                Collectors.counting()
            ));

        // --- Gráfico: Usuarios por Rol ---
        Map<String, Number> datosGrafico = new java.util.HashMap<>();
        rolesCount.forEach((k, v) -> datosGrafico.put(k, v));
        
        String chartBase64 = generarGraficoTortaBase64(datosGrafico, "Distribución por Rol");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        // Periodo Text
        String periodoTexto = "Histórico Completo";
        if (fechaInicio != null && fechaFin != null) {
            periodoTexto = fechaInicio.format(formatter) + " al " + fechaFin.format(formatter);
        } else if (fechaInicio != null) {
            periodoTexto = "Desde " + fechaInicio.format(formatter);
        } else if (fechaFin != null) {
            periodoTexto = "Hasta " + fechaFin.format(formatter);
        }

        // Info usuario generador
        String generadoPor = "Sistema";
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
             generadoPor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // --- Contexto ---
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("usuarios", usuarios);
        data.put("fechaEmision", LocalDate.now().format(formatter));
        data.put("periodoReporte", periodoTexto);
        data.put("generadoPor", generadoPor);
        data.put("reporteId", "RU-" + System.currentTimeMillis());
        data.put("totalUsuariosSistema", totalUsuariosSistema);
        data.put("totalUsuariosReporte", totalUsuariosReporte);
        data.put("usuariosActivos", usuariosActivos);
        data.put("usuariosInactivos", usuariosInactivos);
        data.put("nuevosIngresos", nuevosIngresos);
        data.put("chartImage", chartBase64);
        data.put("estilos", cargarEstilos());

        return generarReportePdfDesdePlantilla("reporte/reporteUsuarios", data);
    }


    private void addTableRow(PdfPTable table, String label, String value) {
        com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        PdfPCell cell1 = new PdfPCell(new Phrase(label, boldFont));
        cell1.setBorder(0);
        cell1.setPadding(5);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, normalFont));
        cell2.setBorder(0);
        cell2.setPadding(5);
        table.addCell(cell2);
    }
}