package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import com.example.demo.model.*;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.enums.EstadoOferta;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.CuotaRepository;
import com.example.demo.repository.InscripcionRepository;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.NotificacionInscripcionRepository;
import com.example.demo.enums.EstadoCuota;
import com.example.demo.enums.EstadoPago;
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
import java.util.Comparator;
import java.awt.image.BufferedImage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.general.DefaultPieDataset;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.example.demo.model.AuditLog;
import com.example.demo.ia.service.ChatServiceSimple;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private OfertaAcademicaRepository ofertaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private NotificacionInscripcionRepository notificacionInscripcionRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private InstitutoService institutoService;

    @Autowired
    private InstitutoLogoService institutoLogoService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TemplateEngine templateEngine; // Thymeleaf Template Engine

    @Autowired
    private ChatServiceSimple chatServiceSimple;

    private String cargarEstilos() {
        try {
            ClassPathResource resource = new ClassPathResource("static/style/reporte_pdf.css");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "body { font-family: Helvetica; }"; // Fallback simple
        }
    }

    private String obtenerLogoBase64(Instituto instituto) {
        if (instituto == null || instituto.getLogoPath() == null || instituto.getLogoPath().isBlank()) {
            return null;
        }
        String path = instituto.getLogoPath().trim();
        if (path.startsWith("/api/instituto/logo/")) {
            try {
                String idPart = path.substring("/api/instituto/logo/".length());
                Long id = Long.parseLong(idPart.trim());
                var logo = institutoLogoService.obtenerPorId(id).orElse(null);
                if (logo == null) return null;
                String mime = logo.getTipoMime() != null ? logo.getTipoMime() : "image/png";
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(logo.getDatos());
            } catch (Exception e) {
                return null;
            }
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            ClassPathResource resource = new ClassPathResource("static/" + path);
            if (!resource.exists()) {
                return null;
            }
            byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
            String lower = path.toLowerCase();
            String mime = "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mime = "image/jpeg";
            } else if (lower.endsWith(".gif")) {
                mime = "image/gif";
            } else if (lower.endsWith(".webp")) {
                mime = "image/webp";
            }
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    private Map<String, String> obtenerDatosGenerador() {
        String nombre = "Sistema";
        String dni = "";
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                String principal = auth.getName();
                if (principal != null && !principal.isBlank()) {
                    Usuario usuario = usuarioRepository.findByDniOrCorreo(principal, principal).orElse(null);
                    if (usuario != null) {
                        nombre = usuario.getNombre() + " " + usuario.getApellido();
                        dni = usuario.getDni();
                    } else {
                        nombre = principal;
                    }
                }
            }
        } catch (Exception e) {
            // Usar valores por defecto
        }
        Map<String, String> data = new java.util.HashMap<>();
        data.put("nombre", nombre);
        data.put("dni", dni);
        return data;
    }

    private void agregarDatosBaseReporte(Map<String, Object> data) {
        Instituto instituto = institutoService.obtenerInstituto();
        String nombreInstituto = instituto != null && instituto.getNombreInstituto() != null && !instituto.getNombreInstituto().isBlank()
                ? instituto.getNombreInstituto()
                : "Instituto";

        List<String> colores = instituto != null ? instituto.getColores() : null;
        String colorPrimario = (colores != null && colores.size() > 0 && colores.get(0) != null && !colores.get(0).isBlank())
                ? colores.get(0) : "#E5383B";
        String colorSecundario = (colores != null && colores.size() > 1 && colores.get(1) != null && !colores.get(1).isBlank())
                ? colores.get(1) : "#0D1B2A";
        String colorTexto = (colores != null && colores.size() > 2 && colores.get(2) != null && !colores.get(2).isBlank())
                ? colores.get(2) : "#374151";

        Map<String, String> gen = obtenerDatosGenerador();

        data.put("instituto", instituto);
        data.put("institutoNombre", nombreInstituto);
        data.put("logoBase64", obtenerLogoBase64(instituto));
        data.put("razonSocial", instituto != null ? instituto.getRazonSocial() : null);
        data.put("cuit", instituto != null ? instituto.getCuil() : null);
        data.put("colorPrimario", colorPrimario);
        data.put("colorSecundario", colorSecundario);
        data.put("colorTexto", colorTexto);
        data.put("generadoPorNombre", gen.get("nombre"));
        data.put("generadoPorDni", gen.get("dni"));
        data.put("generadoPor", gen.get("nombre") + (gen.get("dni") != null && !gen.get("dni").isBlank() ? " (DNI: " + gen.get("dni") + ")" : ""));
    }

    private String calcularMotivoBajaDemanda(OfertaAcademica oferta, int minAlumnos) {
        if (oferta == null) return "Baja por baja demanda";
        int inscripcionesCount = inscripcionRepository.countByOfertaAndEstadoInscripcionTrue(oferta);
        LocalDate hoy = LocalDate.now();
        if (oferta.getFechaInicio() != null) {
            long diasParaInicio = java.time.temporal.ChronoUnit.DAYS.between(hoy, oferta.getFechaInicio());
            if (diasParaInicio >= 1 && diasParaInicio <= 3 && inscripcionesCount <= minAlumnos) {
                return "Inicio inminente (" + diasParaInicio + " dias) con matricula insuficiente (" + inscripcionesCount + " inscritos. Minimo: " + minAlumnos + ")";
            }
        }
        if (oferta.getEstado() == EstadoOferta.ENCURSO && inscripcionesCount < minAlumnos) {
            return "Matricula insuficiente durante el cursado (" + inscripcionesCount + " inscritos. Minimo: " + minAlumnos + ")";
        }
        return "Baja por baja demanda (inscriptos: " + inscripcionesCount + ", minimo: " + minAlumnos + ")";
    }

    private List<Map<String, Object>> obtenerBajasPorAnalisis(List<OfertaAcademica> ofertas) {
        if (ofertas == null || ofertas.isEmpty()) return new java.util.ArrayList<>();
        List<Long> idsFiltrados = ofertas.stream()
            .map(OfertaAcademica::getIdOferta)
            .toList();

        Map<Long, Map<String, Object>> resultById = new java.util.LinkedHashMap<>();
        Map<Long, OfertaAcademica> ofertasById = ofertas.stream()
            .collect(java.util.stream.Collectors.toMap(OfertaAcademica::getIdOferta, o -> o, (a, b) -> a));
        Instituto instituto = institutoService.obtenerInstituto();
        int minAlumnos = instituto != null && instituto.getMinimoAlumnoBaja() != null ? instituto.getMinimoAlumnoBaja() : 5;

        // 1) Bajas automaticas (cron)
        List<AuditLog> autoLogs = auditLogService.obtenerPorAccion("BAJA_AUTOMATICA_OFERTA");
        for (AuditLog log : autoLogs) {
            if (log.getDetalles() == null) continue;
            String detalles = log.getDetalles();
            Long id = null;
            String nombre = null;
            String motivo = null;
            try {
                // Formato: ID:123 | Nombre:xxx | Motivo:yyy
                String[] parts = detalles.split("\\|");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.startsWith("ID:")) {
                        id = Long.parseLong(p.substring(3).trim());
                    } else if (p.startsWith("Nombre:")) {
                        nombre = p.substring(7).trim();
                    } else if (p.startsWith("Motivo:")) {
                        motivo = p.substring(7).trim();
                    }
                }
            } catch (Exception e) {
                // Ignorar parseos invalidos
            }
            if (id != null && idsFiltrados.contains(id) && !resultById.containsKey(id)) {
                Map<String, Object> row = new java.util.HashMap<>();
                OfertaAcademica oferta = ofertasById.get(id);
                row.put("id", id);
                row.put("nombre", nombre != null ? nombre : (oferta != null ? oferta.getNombre() : ("ID " + id)));
                row.put("motivo", calcularMotivoBajaDemanda(oferta, minAlumnos));
                resultById.put(id, row);
            }
        }

        // 2) Bajas confirmadas manualmente desde el analisis (reportes)
        List<AuditLog> manualLogs = auditLogService.obtenerPorAccion("DAR_DE_BAJA_OFERTAS_MANUAL");
        Pattern pattern = Pattern.compile("BAJA APLICADA:\\s*(.*?)\\s*\\(ID:\\s*(\\d+)\\)");
        for (AuditLog log : manualLogs) {
            if (log.getDetalles() == null) continue;
            String detalles = log.getDetalles();
            Matcher matcher = pattern.matcher(detalles);
            while (matcher.find()) {
                String nombre = matcher.group(1).trim();
                Long id = null;
                try {
                    id = Long.parseLong(matcher.group(2));
                } catch (Exception e) {
                    id = null;
                }
                if (id != null && idsFiltrados.contains(id) && !resultById.containsKey(id)) {
                    Map<String, Object> row = new java.util.HashMap<>();
                    OfertaAcademica oferta = ofertasById.get(id);
                    row.put("id", id);
                    row.put("nombre", nombre != null && !nombre.isEmpty() ? nombre : (oferta != null ? oferta.getNombre() : ("ID " + id)));
                    row.put("motivo", calcularMotivoBajaDemanda(oferta, minAlumnos));
                    resultById.put(id, row);
                }
            }
        }

        return new java.util.ArrayList<>(resultById.values());
    }

    /**
     * Genera un PDF a partir de una plantilla HTML Thymeleaf
     */
    public ByteArrayInputStream generarReportePdfDesdePlantilla(String templateName, Map<String, Object> data) {
        log.info("Iniciando generacion de PDF desde plantilla: {}", templateName);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Context context = new Context();
            context.setVariables(data);
            
            log.debug("Procesando plantilla Thymeleaf...");
            String htmlContent = templateEngine.process(templateName, context);
            log.debug("HTML compilado (longitud: {})", htmlContent.length());
            
            // Validar HTML vacio
            if (htmlContent == null || htmlContent.isEmpty()) {
                throw new RuntimeException("La plantilla genero un HTML vacio");
            }

            log.debug("Renderizando PDF con OpenHTMLToPDF...");
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
            
            byte[] pdfBytes = os.toByteArray();
            log.info("PDF generado exitosamente. Tamano: {} bytes", pdfBytes.length);
            
            return new ByteArrayInputStream(pdfBytes);
        } catch (Exception e) {
            log.error("Error FATAL al generar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generando PDF desde plantilla HTML: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un grafico de torta (Pie Chart) y lo devuelve como String Base64 para embeber en HTML
     */
    public String generarGraficoTortaBase64(Map<String, Number> datasetValues, String titulo) {
        try {
            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            for (Map.Entry<String, Number> entry : datasetValues.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart = ChartFactory.createPieChart(
                titulo,   // chart title
                dataset,          // data
                true,             // include legend
                true,
                false);

            // Mostrar valores en las etiquetas del grafico (nombre + cantidad)
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setSimpleLabels(false);
            plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1}"));

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

    /**
     * Genera un grafico de barras (Categorias) y lo devuelve como String Base64
     */
    public String generarGraficoBarrasBase64(Map<String, Number> datasetValues, String titulo) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Number> entry : datasetValues.entrySet()) {
                dataset.addValue(entry.getValue(), "Inscripciones", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    titulo,
                    "Categoria",
                    "Cantidad",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );

            CategoryPlot plot = chart.getCategoryPlot();
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);
            renderer.setShadowVisible(false);
            renderer.setItemMargin(0.05);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int width = 900;
            int height = 420;
            org.jfree.chart.ChartUtils.writeChartAsPNG(baos, chart, width, height);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error al generar grafico de barras", e);
            return null;
        }
    }

    public String generarGraficoBarrasHorizontalesBase64(Map<String, Number> datasetValues, String titulo, String ejeX, String ejeY) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Number> entry : datasetValues.entrySet()) {
                dataset.addValue(entry.getValue(), "Cantidad", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    titulo,
                    ejeX,
                    ejeY,
                    dataset,
                    PlotOrientation.HORIZONTAL,
                    false,
                    true,
                    false
            );

            CategoryPlot plot = chart.getCategoryPlot();
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setLowerBound(0d);

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setDrawBarOutline(false);
            renderer.setShadowVisible(false);
            renderer.setItemMargin(0.08);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int width = 1000;
            int height = 460;
            org.jfree.chart.ChartUtils.writeChartAsPNG(baos, chart, width, height);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error al generar grafico de barras horizontales", e);
            return null;
        }
    }

    public String generarGraficoLineaBase64(Map<String, Number> datasetValues, String titulo, String ejeX, String ejeY) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Number> entry : datasetValues.entrySet()) {
                dataset.addValue(entry.getValue(), "Ingresos", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createLineChart(
                    titulo,
                    ejeX,
                    ejeY,
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );

            CategoryPlot plot = chart.getCategoryPlot();
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setLowerBound(0d);

            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setDefaultShapesVisible(true);
            renderer.setDefaultShapesFilled(true);
            renderer.setDefaultStroke(new java.awt.BasicStroke(2.0f));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.jfree.chart.ChartUtils.writeChartAsPNG(baos, chart, 1000, 460);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error al generar grafico de linea", e);
            return null;
        }
    }

    public List<OfertaAcademica> filtrarOfertas(String nombre, String estado, Long categoriaId, LocalDate fechaInicio, LocalDate fechaFin, List<String> tipos, List<String> modalidades) {
        List<OfertaAcademica> todas = ofertaRepository.findAll();

        final List<String> tiposNormalizados;
        if (tipos == null || tipos.isEmpty()) {
            tiposNormalizados = null;
        } else {
            List<String> normalized = tipos.stream()
                    .map(this::normalizarTipo)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            tiposNormalizados = normalized.isEmpty() ? null : normalized;
        }

        final List<String> modalidadesNormalizadas;
        if (modalidades == null || modalidades.isEmpty()) {
            modalidadesNormalizadas = null;
        } else {
            List<String> normalized = modalidades.stream()
                    .map(this::normalizarTipo)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            modalidadesNormalizadas = normalized.isEmpty() ? null : normalized;
        }

        return todas.stream()
            .filter(o -> nombre == null || nombre.isEmpty() || o.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .filter(o -> estado == null || estado.isEmpty() || (o.getEstado() != null && o.getEstado().name().equalsIgnoreCase(estado)))
            .filter(o -> categoriaId == null || o.getCategorias().stream().anyMatch(c -> c.getIdCategoria().equals(categoriaId)))
            // Filtro temporal por solapamiento (no exige que toda la oferta quede dentro del rango)
            .filter(o -> {
                if (fechaInicio == null) return true;
                LocalDate finOferta = o.getFechaFin() != null ? o.getFechaFin() : o.getFechaInicio();
                return finOferta == null || !finOferta.isBefore(fechaInicio);
            })
            .filter(o -> {
                if (fechaFin == null) return true;
                LocalDate inicioOferta = o.getFechaInicio() != null ? o.getFechaInicio() : o.getFechaFin();
                return inicioOferta == null || !inicioOferta.isAfter(fechaFin);
            })
            .filter(o -> tiposNormalizados == null || tiposNormalizados.contains(normalizarTipo(o.getTipoOferta())))
            .filter(o -> modalidadesNormalizadas == null
                    || (o.getModalidad() != null && modalidadesNormalizadas.contains(normalizarTipo(o.getModalidad().name()))))
            .collect(Collectors.toList());
    }

    private String normalizarTipo(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toUpperCase(Locale.ROOT);
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
            Sheet sheet = workbook.createSheet("Ofertas Academicas");

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
                row.createCell(4).setCellValue(oferta.getFechaInicio() != null ? oferta.getFechaInicio().format(REPORT_DATE_FORMAT) : "");
                row.createCell(5).setCellValue(oferta.getFechaFin() != null ? oferta.getFechaFin().format(REPORT_DATE_FORMAT) : "");
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
        // Configuramos margenes: Izq, Der, Arr, Abajo (para dar espacio a encabezado/pie)
        Document document = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // --- DEFINICION DE EVENTOS DE PAGINA (Encabezado y Pie) ---
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

                    // --- PIE DE PAGINA CONSISTENTE ---
                    cb.saveState();
                    cb.setLineWidth(0.5f);
                    cb.moveTo(40, 30);
                    cb.lineTo(width - 40, 30);
                    cb.stroke();
                    
                    cb.beginText();
                    cb.setFontAndSize(bf, 9);
                    
                    cb.setTextMatrix(40, 20);
                    cb.showText("Informe Confidencial - Uso Interno");
                    
                    String text = "Pagina " + writer.getPageNumber() + " de ";
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

            // --- 1. TITULO CON JERARQUIA VISUAL ---
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Ofertas Academicas y Estadisticas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);
            
            // Subtitulo descriptivo
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
        addSummarySection(doc, ofertas, null, null);
    }

    private void addSummarySection(Document doc, List<OfertaAcademica> ofertas, LocalDate fechaInicio, LocalDate fechaFin) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10f);
        summaryTable.setSpacingAfter(20f);
        summaryTable.getDefaultCell().setBorder(0);

        java.util.Set<Long> ofertasIncluidas = ofertas.stream()
                .map(OfertaAcademica::getIdOferta)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<com.example.demo.model.Inscripciones> inscripcionesFiltradas = inscripcionRepository.findAll().stream()
                .filter(i -> i.getOferta() != null && i.getOferta().getIdOferta() != null)
                .filter(i -> ofertasIncluidas.contains(i.getOferta().getIdOferta()))
                .filter(i -> i.getFechaInscripcion() != null)
                .filter(i -> fechaInicio == null || !i.getFechaInscripcion().isBefore(fechaInicio))
                .filter(i -> fechaFin == null || !i.getFechaInscripcion().isAfter(fechaFin))
                .collect(Collectors.toList());

        int totalOfertas = (fechaInicio != null || fechaFin != null)
                ? (int) inscripcionesFiltradas.stream().map(i -> i.getOferta().getIdOferta()).distinct().count()
                : ofertas.size();
        long totalInscritos = inscripcionesFiltradas.size();
        long activos = inscripcionesFiltradas.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion()))
                .count();
        double ingresos = inscripcionesFiltradas.stream()
                .mapToDouble(i -> i.getOferta().getCostoInscripcion() != null ? i.getOferta().getCostoInscripcion() : 0.0)
                .sum();

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
        // Configuracion de tabla para legibilidad
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
            addCell(table, oferta.getFechaInicio() != null ? oferta.getFechaInicio().format(REPORT_DATE_FORMAT) : "", dataFont, bgColor, Element.ALIGN_CENTER);
            addCell(table, oferta.getFechaFin() != null ? oferta.getFechaFin().format(REPORT_DATE_FORMAT) : "", dataFont, bgColor, Element.ALIGN_CENTER);
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

    public ByteArrayInputStream generarReporteEstadisticoPDF(List<OfertaAcademica> ofertas, LocalDate fechaInicio, LocalDate fechaFin, String agrupacion) {
        List<OfertaAcademica> ofertasSafe = ofertas != null ? ofertas : java.util.Collections.emptyList();
        Instituto instituto = institutoService.obtenerInstituto();
        String nombreInstitucion = (instituto != null && instituto.getNombreInstituto() != null && !instituto.getNombreInstituto().isBlank())
                ? instituto.getNombreInstituto().trim()
                : "la institucion";

        String periodoTexto = "Historico completo";
        if (fechaInicio != null && fechaFin != null) {
            periodoTexto = fechaInicio.format(REPORT_DATE_FORMAT) + " al " + fechaFin.format(REPORT_DATE_FORMAT);
        } else if (fechaInicio != null) {
            periodoTexto = "Desde " + fechaInicio.format(REPORT_DATE_FORMAT);
        } else if (fechaFin != null) {
            periodoTexto = "Hasta " + fechaFin.format(REPORT_DATE_FORMAT);
        }

        java.util.Set<Long> ofertaIds = ofertasSafe.stream()
                .map(OfertaAcademica::getIdOferta)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<com.example.demo.model.Inscripciones> inscripcionesPeriodo = inscripcionRepository.findAll().stream()
                .filter(i -> i.getOferta() != null && i.getOferta().getIdOferta() != null)
                .filter(i -> ofertaIds.contains(i.getOferta().getIdOferta()))
                .filter(i -> i.getFechaInscripcion() != null)
                .filter(i -> fechaInicio == null || !i.getFechaInscripcion().isBefore(fechaInicio))
                .filter(i -> fechaFin == null || !i.getFechaInscripcion().isAfter(fechaFin))
                .collect(Collectors.toList());

        long totalProgramas = (fechaInicio != null || fechaFin != null)
                ? inscripcionesPeriodo.stream().map(i -> i.getOferta().getIdOferta()).distinct().count()
                : ofertasSafe.size();
        long totalInscripciones = inscripcionesPeriodo.size();
        long alumnosActivos = inscripcionesPeriodo.stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count();
        double ingresosEstimados = inscripcionesPeriodo.stream()
                .mapToDouble(i -> i.getOferta().getCostoInscripcion() != null ? i.getOferta().getCostoInscripcion() : 0.0)
                .sum();

        Map<String, Long> ofertasPorTipo = inscripcionesPeriodo.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getOferta().getTipoOferta() != null ? i.getOferta().getTipoOferta() : "Sin tipo",
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, Long> ofertasPorModalidad = inscripcionesPeriodo.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getOferta().getModalidad() != null ? i.getOferta().getModalidad().name() : "SIN_MODALIDAD",
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, Long> ofertasPorEstado = inscripcionesPeriodo.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getOferta().getEstado() != null ? i.getOferta().getEstado().name() : "SIN_ESTADO",
                        LinkedHashMap::new,
                        Collectors.counting()));

        if (ofertasPorTipo.isEmpty()) ofertasPorTipo = java.util.Map.of("Sin datos", 0L);
        if (ofertasPorModalidad.isEmpty()) ofertasPorModalidad = java.util.Map.of("Sin datos", 0L);
        if (ofertasPorEstado.isEmpty()) ofertasPorEstado = java.util.Map.of("Sin datos", 0L);

        Map<String, Number> chartTipoData = new LinkedHashMap<>();
        ofertasPorTipo.forEach(chartTipoData::put);
        Map<String, Number> chartModalidadData = new LinkedHashMap<>();
        ofertasPorModalidad.forEach(chartModalidadData::put);
        Map<String, Number> chartEstadoData = new LinkedHashMap<>();
        ofertasPorEstado.forEach(chartEstadoData::put);

        String chartTipo = generarGraficoTortaBase64(chartTipoData, "Inscripciones por tipo");
        String chartModalidad = generarGraficoTortaBase64(chartModalidadData, "Inscripciones por modalidad");
        String chartEstado = generarGraficoTortaBase64(chartEstadoData, "Inscripciones por estado");

        List<Usuario> todosAlumnos = usuarioRepository.findByRolesNombre("ALUMNO");
        long totalAlumnos = todosAlumnos.size();
        long alumnosAlta = todosAlumnos.stream().filter(Usuario::isEstado).count();
        long alumnosBaja = totalAlumnos - alumnosAlta;

        List<Usuario> todosDocentes = usuarioRepository.findByRolesNombre("DOCENTE");
        long totalDocentes = todosDocentes.size();
        long docentesAlta = todosDocentes.stream().filter(Usuario::isEstado).count();
        long docentesBaja = totalDocentes - docentesAlta;

        java.util.Set<java.util.UUID> docentesAsignadosIds = new java.util.HashSet<>();
        for (OfertaAcademica o : ofertasSafe) {
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

        List<Usuario> todosAdmins = usuarioRepository.findByRolesNombre("ADMIN");
        long adminsAlta = todosAdmins.stream().filter(Usuario::isEstado).count();
        long totalUsuariosSistema = usuarioRepository.count();

        long totalCupos = ofertasSafe.stream().mapToLong(o -> o.getCupos() != null && o.getCupos() < 10000 ? o.getCupos() : 0).sum();
        long inscritosEnOfertasConCupo = ofertasSafe.stream()
                .filter(o -> o.getCupos() != null && o.getCupos() < 10000)
                .mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0)
                .sum();
        double tasaOcupacion = totalCupos > 0 ? (double) inscritosEnOfertasConCupo / totalCupos * 100 : 0;

        long ofertasBajaDemanda = ofertasSafe.stream()
                .filter(o -> o.getInscripciones() != null && o.getInscripciones().size() < 5)
                .count();

        Map<Long, Long> inscripcionesPorOfertaPeriodo = inscripcionesPeriodo.stream()
                .collect(Collectors.groupingBy(i -> i.getOferta().getIdOferta(), Collectors.counting()));

        List<String> ofertasMenorInscripcion = ofertasSafe.stream()
                .sorted(Comparator.comparingLong(o -> inscripcionesPorOfertaPeriodo.getOrDefault(o.getIdOferta(), 0L)))
                .limit(3)
                .map(o -> o.getNombre() + " (" + inscripcionesPorOfertaPeriodo.getOrDefault(o.getIdOferta(), 0L) + ")")
                .collect(Collectors.toList());

        List<String> ofertasMayorIngreso = ofertasSafe.stream()
                .sorted((a, b) -> Double.compare(
                        calcularIngresoOfertaPeriodo(b, inscripcionesPorOfertaPeriodo),
                        calcularIngresoOfertaPeriodo(a, inscripcionesPorOfertaPeriodo)))
                .limit(3)
                .map(o -> o.getNombre() + " ($ " + String.format(Locale.US, "%.2f",
                        calcularIngresoOfertaPeriodo(o, inscripcionesPorOfertaPeriodo)) + ")")
                .collect(Collectors.toList());

        LocalDate hoy = LocalDate.now();
        LocalDate rangoInicio = fechaInicio != null ? fechaInicio : hoy.minusDays(30);
        LocalDate rangoFin = fechaFin != null ? fechaFin : hoy;
        if (rangoInicio.isAfter(rangoFin)) {
            LocalDate aux = rangoInicio;
            rangoInicio = rangoFin;
            rangoFin = aux;
        }

        String tipoComparacion = resolverTipoComparacion(agrupacion, rangoInicio, rangoFin);
        LocalDate[] periodoAnterior = calcularPeriodoAnterior(rangoInicio, rangoFin, tipoComparacion);
        LocalDate inicioAnterior = periodoAnterior[0];
        LocalDate finAnterior = periodoAnterior[1];

        long alumnosNuevosActual = contarUsuariosNuevosEnPeriodo("ALUMNO", rangoInicio, rangoFin);
        long docentesNuevosActual = contarUsuariosNuevosEnPeriodo("DOCENTE", rangoInicio, rangoFin);
        long adminsNuevosActual = contarUsuariosNuevosEnPeriodo("ADMIN", rangoInicio, rangoFin);

        long alumnosNuevosAnterior = contarUsuariosNuevosEnPeriodo("ALUMNO", inicioAnterior, finAnterior);
        long docentesNuevosAnterior = contarUsuariosNuevosEnPeriodo("DOCENTE", inicioAnterior, finAnterior);
        long adminsNuevosAnterior = contarUsuariosNuevosEnPeriodo("ADMIN", inicioAnterior, finAnterior);

        List<com.example.demo.model.Inscripciones> inscripcionesAnterior = inscripcionRepository.findAll().stream()
                .filter(i -> i.getOferta() != null && i.getOferta().getIdOferta() != null)
                .filter(i -> ofertaIds.contains(i.getOferta().getIdOferta()))
                .filter(i -> i.getFechaInscripcion() != null)
                .filter(i -> !i.getFechaInscripcion().isBefore(inicioAnterior) && !i.getFechaInscripcion().isAfter(finAnterior))
                .collect(Collectors.toList());
        long programasAnterior = inscripcionesAnterior.stream()
                .map(i -> i.getOferta().getIdOferta())
                .distinct()
                .count();

        List<Map<String, Object>> poblacionHeatmap = new java.util.ArrayList<>();
        poblacionHeatmap.add(crearFilaHeatmap("Altas de alumnos", alumnosNuevosActual, alumnosNuevosAnterior));
        poblacionHeatmap.add(crearFilaHeatmap("Altas de docentes", docentesNuevosActual, docentesNuevosAnterior));
        poblacionHeatmap.add(crearFilaHeatmap("Altas de admins", adminsNuevosActual, adminsNuevosAnterior));
        poblacionHeatmap.add(crearFilaHeatmap("Inscripciones", totalInscripciones, inscripcionesAnterior.size()));
        poblacionHeatmap.add(crearFilaHeatmap("Programas con actividad", totalProgramas, programasAnterior));

        String etiquetaComparacion = switch (tipoComparacion) {
            case "semana" -> "semana anterior";
            case "mes" -> "mes anterior";
            case "anio" -> "anio anterior";
            default -> "periodo anterior equivalente";
        };
        String periodoActualTxt = rangoInicio.format(REPORT_DATE_FORMAT) + " al " + rangoFin.format(REPORT_DATE_FORMAT);
        String periodoAnteriorTxt = inicioAnterior.format(REPORT_DATE_FORMAT) + " al " + finAnterior.format(REPORT_DATE_FORMAT);
        String narrativaComparativaBase = construirNarrativaComparativa(
                etiquetaComparacion, periodoActualTxt, periodoAnteriorTxt,
                totalInscripciones, inscripcionesAnterior.size(),
                alumnosNuevosActual, alumnosNuevosAnterior,
                docentesNuevosActual, docentesNuevosAnterior,
                adminsNuevosActual, adminsNuevosAnterior);

        Map<String, Object> payloadNarrativaIA = new HashMap<>();
        payloadNarrativaIA.put("institucion", nombreInstitucion);
        payloadNarrativaIA.put("periodoActual", periodoActualTxt);
        payloadNarrativaIA.put("periodoAnterior", periodoAnteriorTxt);
        payloadNarrativaIA.put("comparacionContra", etiquetaComparacion);
        payloadNarrativaIA.put("inscripcionesActual", totalInscripciones);
        payloadNarrativaIA.put("inscripcionesAnterior", inscripcionesAnterior.size());
        payloadNarrativaIA.put("altasAlumnosActual", alumnosNuevosActual);
        payloadNarrativaIA.put("altasAlumnosAnterior", alumnosNuevosAnterior);
        payloadNarrativaIA.put("altasDocentesActual", docentesNuevosActual);
        payloadNarrativaIA.put("altasDocentesAnterior", docentesNuevosAnterior);
        payloadNarrativaIA.put("altasAdminsActual", adminsNuevosActual);
        payloadNarrativaIA.put("altasAdminsAnterior", adminsNuevosAnterior);

        String narrativaComparativaIA = null;
        try {
            narrativaComparativaIA = chatServiceSimple.generarNarrativaComparativaEstadistica(payloadNarrativaIA);
        } catch (Exception ignored) {
        }
        String narrativaComparativa = (narrativaComparativaIA != null && !narrativaComparativaIA.isBlank())
                ? narrativaComparativaIA
                : narrativaComparativaBase;

        double coberturaDocente = docentesAlta > 0 ? (docentesConAsignacion * 100.0 / docentesAlta) : 0.0;
        String detalleBajaInscripcion = ofertasMenorInscripcion.isEmpty()
                ? "No se detectaron ofertas con baja inscripcion."
                : "Ofertas con menor inscripcion en el periodo: " + String.join(", ", ofertasMenorInscripcion) + ".";
        String detalleIngresos = ofertasMayorIngreso.isEmpty()
                ? "No hubo ingresos registrados por ofertas en el periodo."
                : "Ofertas con mayores ingresos: " + String.join(", ", ofertasMayorIngreso) + ".";

        String observaciones = "Este reporte de " + nombreInstitucion + " refleja la situacion del periodo analizado. "
                + "La tasa de ocupacion general fue " + String.format(Locale.US, "%.1f%%", tasaOcupacion) + " y la cobertura docente activa fue "
                + String.format(Locale.US, "%.1f%%", coberturaDocente) + " (" + docentesConAsignacion + " de " + docentesAlta + " docentes activos con asignacion). "
                + detalleBajaInscripcion + " "
                + detalleIngresos + " "
                + "Se recomienda reforzar la difusion en ofertas de baja inscripcion y sostener las que muestran mayor retorno.";

        Map<String, Object> data = new HashMap<>();
        data.put("fechaEmision", LocalDate.now().format(REPORT_DATE_FORMAT));
        data.put("nombreInstitucion", nombreInstitucion);
        data.put("periodoReporte", periodoTexto);
        data.put("reporteId", "RE-" + System.currentTimeMillis());
        data.put("totalProgramas", totalProgramas);
        data.put("totalInscripciones", totalInscripciones);
        data.put("alumnosActivos", alumnosActivos);
        data.put("ingresosEstimados", String.format("$ %.2f", ingresosEstimados));
        data.put("ofertasPorTipo", ofertasPorTipo);
        data.put("ofertasPorModalidad", ofertasPorModalidad);
        data.put("ofertasPorEstado", ofertasPorEstado);
        data.put("chartTipo", chartTipo);
        data.put("chartModalidad", chartModalidad);
        data.put("chartEstado", chartEstado);
        data.put("totalAlumnos", totalAlumnos);
        data.put("alumnosAlta", alumnosAlta);
        data.put("alumnosBaja", alumnosBaja);
        data.put("totalDocentes", totalDocentes);
        data.put("docentesAlta", docentesAlta);
        data.put("docentesBaja", docentesBaja);
        data.put("docentesConAsignacion", docentesConAsignacion);
        data.put("adminsAlta", adminsAlta);
        data.put("totalUsuariosSistema", totalUsuariosSistema);
        data.put("tasaOcupacion", String.format("%.1f %%", tasaOcupacion));
        data.put("ofertasBajaDemanda", ofertasBajaDemanda);
        data.put("observaciones", observaciones);
        data.put("poblacionHeatmap", poblacionHeatmap);
        data.put("narrativaComparativa", narrativaComparativa);
        data.put("periodoComparadoEtiqueta", etiquetaComparacion);
        data.put("periodoActualTexto", periodoActualTxt);
        data.put("periodoAnteriorTexto", periodoAnteriorTxt);
        data.put("estilos", cargarEstilos());
        agregarDatosBaseReporte(data);

        return generarReportePdfDesdePlantilla("reporte/reporteEstadistico", data);
    }

    private String resolverTipoComparacion(String agrupacion, LocalDate inicio, LocalDate fin) {
        String agr = agrupacion != null ? agrupacion.trim().toLowerCase(Locale.ROOT) : "";
        if ("semana".equals(agr)) return "semana";
        if ("mes".equals(agr)) return "mes";
        if ("anio".equals(agr) || "ano".equals(agr) || "year".equals(agr)) return "anio";

        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) + 1;
        if (dias <= 8) return "semana";
        if (dias <= 35) return "mes";
        if (dias >= 330) return "anio";
        return "periodo";
    }

    private LocalDate[] calcularPeriodoAnterior(LocalDate inicio, LocalDate fin, String tipoComparacion) {
        return switch (tipoComparacion) {
            case "semana" -> new LocalDate[]{inicio.minusWeeks(1), fin.minusWeeks(1)};
            case "mes" -> new LocalDate[]{inicio.minusMonths(1), fin.minusMonths(1)};
            case "anio" -> new LocalDate[]{inicio.minusYears(1), fin.minusYears(1)};
            default -> {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin) + 1;
                LocalDate finAnt = inicio.minusDays(1);
                LocalDate iniAnt = finAnt.minusDays(Math.max(dias - 1, 0));
                yield new LocalDate[]{iniAnt, finAnt};
            }
        };
    }

    private long contarUsuariosNuevosEnPeriodo(String rol, LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) return 0;
        return usuarioRepository.findByRolesNombre(rol).stream()
                .filter(u -> u.getFechaRegistro() != null)
                .map(u -> u.getFechaRegistro().toLocalDate())
                .filter(f -> !f.isBefore(inicio) && !f.isAfter(fin))
                .count();
    }

    private Map<String, Object> crearFilaHeatmap(String indicador, long actual, long anterior) {
        double variacion = anterior > 0 ? ((double) (actual - anterior) / anterior) * 100.0 : (actual > 0 ? 100.0 : 0.0);
        double intensidad = Math.min(Math.abs(variacion), 100.0) / 100.0;
        String fondo;
        String texto = "#0f172a";
        if (variacion >= 0) {
            fondo = "rgba(34, 197, 94, " + String.format(Locale.US, "%.2f", 0.12 + intensidad * 0.55) + ")";
        } else {
            fondo = "rgba(239, 68, 68, " + String.format(Locale.US, "%.2f", 0.12 + intensidad * 0.55) + ")";
        }
        Map<String, Object> fila = new HashMap<>();
        fila.put("indicador", indicador);
        fila.put("actual", actual);
        fila.put("anterior", anterior);
        fila.put("variacion", String.format(Locale.US, "%+.1f%%", variacion));
        fila.put("bgColor", fondo);
        fila.put("textColor", texto);
        return fila;
    }

    private String construirNarrativaComparativa(
            String etiquetaComparacion,
            String periodoActualTxt,
            String periodoAnteriorTxt,
            long inscripcionesActual,
            long inscripcionesAnterior,
            long alumnosActual,
            long alumnosAnterior,
            long docentesActual,
            long docentesAnterior,
            long adminsActual,
            long adminsAnterior) {
        double varInsc = inscripcionesAnterior > 0
                ? ((double) (inscripcionesActual - inscripcionesAnterior) / inscripcionesAnterior) * 100.0
                : (inscripcionesActual > 0 ? 100.0 : 0.0);
        String tendenciaInsc = varInsc >= 0 ? "crecimiento" : "caida";

        return "Comparativa automatica contra " + etiquetaComparacion + ". "
                + "Periodo actual: " + periodoActualTxt + ". "
                + "Periodo anterior: " + periodoAnteriorTxt + ". "
                + "Inscripciones: " + inscripcionesActual + " vs " + inscripcionesAnterior + " (" + String.format(Locale.US, "%+.1f%%", varInsc) + ", " + tendenciaInsc + "). "
                + "Altas de alumnos: " + alumnosActual + " vs " + alumnosAnterior + ". "
                + "Altas de docentes: " + docentesActual + " vs " + docentesAnterior + ". "
                + "Altas de administradores: " + adminsActual + " vs " + adminsAnterior + ".";
    }

    private double calcularIngresoOfertaPeriodo(OfertaAcademica oferta, Map<Long, Long> inscripcionesPorOfertaPeriodo) {
        if (oferta == null || oferta.getIdOferta() == null) return 0.0;
        long cantidad = inscripcionesPorOfertaPeriodo.getOrDefault(oferta.getIdOferta(), 0L);
        double costo = oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0;
        return cantidad * costo;
    }

    private void addChartSection(Document doc, String chartTitle, java.util.Map<String, Long> data, java.awt.Color barColor) throws DocumentException {
        PdfPTable container = new PdfPTable(1);
        container.setWidthPercentage(100);
        container.getDefaultCell().setBorder(0);
        
        // Header del Grafico
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

        if (data == null || data.isEmpty()) {
            data = java.util.Map.of("Sin datos", 0L);
        }

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
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Estilos
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            com.lowagie.text.Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            com.lowagie.text.Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.lowagie.text.Font whiteSmall = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.WHITE);
            com.lowagie.text.Font whiteBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);

            // Datos del emisor (parametrizable)
            Instituto instituto = null;
            if (pago.getOferta() != null && pago.getOferta().getInstituto() != null) {
                instituto = pago.getOferta().getInstituto();
            } else {
                try {
                    instituto = institutoService.obtenerInstituto();
                } catch (Exception e) {
                    instituto = null;
                }
            }
            String razonSocial = instituto != null && instituto.getRazonSocial() != null && !instituto.getRazonSocial().isBlank()
                ? instituto.getRazonSocial()
                : (instituto != null && instituto.getNombreInstituto() != null ? instituto.getNombreInstituto() : "AUREA");
            String nombreInstituto = instituto != null && instituto.getNombreInstituto() != null && !instituto.getNombreInstituto().isBlank()
                ? instituto.getNombreInstituto()
                : "Instituto";
            String cuit = instituto != null && instituto.getCuil() != null ? instituto.getCuil() : "N/A";
            LocalDateTime inicioActividad = instituto != null ? instituto.getInicioActividad() : null;
            Color primary = new Color(12, 24, 43);
            Color secondary = new Color(23, 45, 84);
            Color text = new Color(39, 53, 78);

            LocalDateTime fechaEmision = LocalDateTime.now();
            LocalDateTime fechaPago = pago.getFechaAprobacion() != null ? pago.getFechaAprobacion() : pago.getFechaPago();
            String comprobanteCodigo = pago.getComprobante() != null && !pago.getComprobante().isBlank()
                ? pago.getComprobante()
                : "CP-" + pago.getIdPago() + "-" + fechaEmision.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

            // Header estilo ejemplo
            PdfPTable header = new PdfPTable(new float[]{3.5f, 2.5f});
            header.setWidthPercentage(100);
            header.setSpacingAfter(12f);

            PdfPCell headerLeft = new PdfPCell();
            headerLeft.setBorder(0);
            headerLeft.setPadding(12);
            headerLeft.setBackgroundColor(primary);

            PdfPTable brand = new PdfPTable(new float[]{1f, 4f});
            brand.setWidthPercentage(100);
            PdfPCell brandLogo = new PdfPCell();
            brandLogo.setBorder(0);
            brandLogo.setPadding(2);
            try {
                String logoBase64 = obtenerLogoBase64(instituto);
                if (logoBase64 != null && logoBase64.contains("base64,")) {
                    String base64Data = logoBase64.substring(logoBase64.indexOf("base64,") + 7);
                    byte[] logoBytes = Base64.getDecoder().decode(base64Data);
                    com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(logoBytes);
                    img.scaleToFit(42, 42);
                    brandLogo.addElement(img);
                }
            } catch (Exception ignored) {
            }
            brand.addCell(brandLogo);
            PdfPCell brandText = new PdfPCell();
            brandText.setBorder(0);
            brandText.addElement(new Paragraph(nombreInstituto, whiteBold));
            brandText.addElement(new Paragraph("CUIT: " + cuit, whiteSmall));
            brand.addCell(brandText);
            headerLeft.addElement(brand);
            header.addCell(headerLeft);

            PdfPCell headerRight = new PdfPCell();
            headerRight.setBorder(0);
            headerRight.setPadding(12);
            headerRight.setBackgroundColor(primary);
            Paragraph compTitle = new Paragraph("COMPROBANTE", whiteBold);
            compTitle.setAlignment(Element.ALIGN_RIGHT);
            headerRight.addElement(compTitle);
            Paragraph compCode = new Paragraph("# " + comprobanteCodigo, whiteSmall);
            compCode.setAlignment(Element.ALIGN_RIGHT);
            headerRight.addElement(compCode);
            header.addCell(headerRight);

            document.add(header);

            // Info bloques: emitido / detalles / estado
            PdfPTable info = new PdfPTable(new float[]{2.4f, 2.4f, 1.2f});
            info.setWidthPercentage(100);
            info.setSpacingAfter(12f);

            PdfPCell emitido = new PdfPCell();
            emitido.setBorderColor(text);
            emitido.setPadding(8);
            Paragraph emitTitle = new Paragraph("EMITIDO PARA", headerFont);
            emitTitle.setSpacingAfter(6f);
            emitido.addElement(emitTitle);
            String alumnoNombre = pago.getUsuario() != null ? pago.getUsuario().getNombre() + " " + pago.getUsuario().getApellido() : "N/A";
            emitido.addElement(new Paragraph(alumnoNombre, normalFont));
            emitido.addElement(new Paragraph((pago.getEmailPagador() != null ? pago.getEmailPagador() : "N/A"), smallFont));
            emitido.addElement(new Paragraph("DNI: " + (pago.getUsuario() != null ? pago.getUsuario().getDni() : "N/A"), smallFont));
            info.addCell(emitido);

            PdfPCell detalles = new PdfPCell();
            detalles.setBorderColor(text);
            detalles.setPadding(8);
            Paragraph detTitle = new Paragraph("DETALLES DEL PAGO", headerFont);
            detTitle.setSpacingAfter(6f);
            detalles.addElement(detTitle);
            detalles.addElement(new Paragraph("Fecha: " + (fechaPago != null ? fechaPago.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"), smallFont));
            detalles.addElement(new Paragraph("Emision: " + fechaEmision.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smallFont));
            detalles.addElement(new Paragraph("Metodo: " + formatearMetodoPago(pago.getTipoPago()), smallFont));
            detalles.addElement(new Paragraph("Ref: " + (pago.getExternalReference() != null ? pago.getExternalReference() : "N/A"), smallFont));
            detalles.addElement(new Paragraph("Transaccion: " + (pago.getPaymentId() != null ? pago.getPaymentId().toString() : "N/A"), smallFont));
            info.addCell(detalles);

            PdfPCell estado = new PdfPCell();
            estado.setBorderColor(text);
            estado.setPadding(8);
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(100);
            PdfPCell badge = new PdfPCell(new Phrase("PAGADO", whiteBold));
            badge.setBorder(0);
            badge.setBackgroundColor(secondary);
            badge.setHorizontalAlignment(Element.ALIGN_CENTER);
            badge.setPadding(6);
            badgeTable.addCell(badge);
            estado.addElement(badgeTable);
            info.addCell(estado);

            document.add(info);

            // Datos del instituto antes de la descripcion
            PdfPTable institutoTable = new PdfPTable(2);
            institutoTable.setWidthPercentage(100);
            institutoTable.setSpacingAfter(12f);
            addTableRow(institutoTable, "Razon Social:", razonSocial);
            addTableRow(institutoTable, "CUIT:", cuit);
            addTableRow(institutoTable, "Inicio Actividad:", inicioActividad != null ? inicioActividad.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
            Paragraph instTitle = new Paragraph("DATOS DEL INSTITUTO", headerFont);
            instTitle.setSpacingAfter(6f);
            document.add(instTitle);
            document.add(institutoTable);

            // Tabla de conceptos
            PdfPTable items = new PdfPTable(new float[]{4.2f, 1f, 1.4f, 1.4f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(12f);
            addHeaderCell(items, "Descripcion", secondary, Color.WHITE);
            addHeaderCell(items, "Cant.", secondary, Color.WHITE);
            addHeaderCell(items, "Unitario", secondary, Color.WHITE);
            addHeaderCell(items, "Total", secondary, Color.WHITE);
            String concepto = pago.getDescripcion() != null ? pago.getDescripcion() : "Inscripcion";
            String oferta = pago.getOferta() != null ? " (" + pago.getOferta().getNombre() + ")" : "";
            addBodyCell(items, concepto + oferta, normalFont);
            addBodyCell(items, "1", normalFont, Element.ALIGN_CENTER);
            addBodyCell(items, formatearMontoComa(pago.getMonto()), normalFont, Element.ALIGN_RIGHT);
            addBodyCell(items, formatearMontoComa(pago.getMonto()), normalFont, Element.ALIGN_RIGHT);
            document.add(items);

            // Totales
            PdfPTable totals = new PdfPTable(new float[]{4f, 2f});
            totals.setWidthPercentage(100);
            addTotalRow(totals, "Subtotal", formatearMontoComa(pago.getMonto()), normalFont);
            addTotalRow(totals, "Impuestos", "$ 0,00", normalFont);
            addTotalRow(totals, "Descuento", "$ 0,00", normalFont);
            PdfPCell totalLabel = new PdfPCell(new Phrase("Total", headerFont));
            totalLabel.setBorder(0);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setPadding(6);
            PdfPCell totalValue = new PdfPCell(new Phrase(formatearMontoComa(pago.getMonto()), titleFont));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setBorder(0);
            totalValue.setPadding(6);
            totals.addCell(totalLabel);
            totals.addCell(totalValue);
            document.add(totals);

            // Footer
            Paragraph footer = new Paragraph("Comprobante generado automaticamente por " + nombreInstituto + ".", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(16f);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Version HTML+CSS (Profesional) del reporte de pagos
     */
    public ByteArrayInputStream generarReportePagosPDF(List<Pago> pagos, Long filtroCursoId) {
        // Calcular estadisticas: recaudado solo de pagos efectivamente completados
        double totalMonto = pagos.stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.COMPLETADO)
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0)
                .sum();
        
        // --- 1. Calcular Morosidad (Cuotas vencidas) ---
        LocalDate today = LocalDate.now();
        List<com.example.demo.model.Cuota> allCuotas = cuotaRepository.findAll();
        
        List<com.example.demo.model.Cuota> cuotasVencidas = allCuotas.stream()
            .filter(c -> c.getEstado() == EstadoCuota.PENDIENTE)
            .filter(c -> c.getFechaVencimiento() != null && c.getFechaVencimiento().isBefore(today))
            .filter(c -> filtroCursoId == null || (c.getInscripcion() != null && c.getInscripcion().getOferta() != null && c.getInscripcion().getOferta().getIdOferta().equals(filtroCursoId)))
            .collect(Collectors.toList());

        long morososCount = cuotasVencidas.stream()
            .filter(c -> c.getInscripcion() != null && c.getInscripcion().getAlumno() != null
                    && c.getInscripcion().getAlumno().getId() != null)
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
                   String alumno = "-";
                   if (c.getInscripcion() != null && c.getInscripcion().getAlumno() != null) {
                       String nombre = c.getInscripcion().getAlumno().getNombre() != null ? c.getInscripcion().getAlumno().getNombre() : "";
                       String apellido = c.getInscripcion().getAlumno().getApellido() != null ? c.getInscripcion().getAlumno().getApellido() : "";
                       String completo = (nombre + " " + apellido).trim();
                       alumno = completo.isBlank() ? "-" : completo;
                   }
                   map.put("alumno", alumno);
                   map.put("curso", (c.getInscripcion() != null && c.getInscripcion().getOferta() != null)
                           ? c.getInscripcion().getOferta().getNombre() : "N/A");
                   map.put("cuota", "C-" + c.getNumeroCuota());
                   map.put("vencimiento", c.getFechaVencimiento() != null
                           ? c.getFechaVencimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                           : "-");
                   long diasAtraso = c.getFechaVencimiento() != null
                           ? java.time.temporal.ChronoUnit.DAYS.between(c.getFechaVencimiento(), today)
                           : 0L;
                   map.put("diasAtraso", Math.max(diasAtraso, 0L));
                   map.put("monto", String.format("$ %.2f", c.getMonto() != null ? c.getMonto().doubleValue() : 0.0));
                   // Calcular interes simple del 5% como regla de negocio de ejemplo para el reporte
                   double interes = (c.getMonto() != null ? c.getMonto().doubleValue() * 0.05 : 0.0);
                   map.put("interes", String.format("$ %.2f", interes));
                   return map;
               }).collect(Collectors.toList());

        // Preparar datos para el grafico
        Map<String, Number> datosGrafico = pagos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstadoPago().toString(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String chartBase64 = generarGraficoTortaBase64(datosGrafico, "Distribucion por Estado");

        // Graficos temporales para seguimiento financiero
        List<Pago> pagosCompletados = pagos.stream()
                .filter(p -> p.getEstadoPago() == EstadoPago.COMPLETADO && p.getFechaPago() != null)
                .collect(Collectors.toList());
        YearMonth mesActual = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);
        Map<String, Number> datosComparativoMes = new LinkedHashMap<>();
        double totalMesAnterior = pagosCompletados.stream()
                .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesAnterior))
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                .sum();
        double totalMesActual = pagosCompletados.stream()
                .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesActual))
                .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                .sum();
        datosComparativoMes.put(mesAnterior.getMonth().name() + " " + mesAnterior.getYear(), totalMesAnterior);
        datosComparativoMes.put(mesActual.getMonth().name() + " " + mesActual.getYear(), totalMesActual);
        String chartComparativoMesBase64 = generarGraficoBarrasBase64(datosComparativoMes, "Comparativo mensual");

        Map<String, Number> datosLineaIngresos = new LinkedHashMap<>();
        for (int d = 1; d <= mesActual.lengthOfMonth(); d++) {
            final int dia = d;
            double montoDia = pagosCompletados.stream()
                    .filter(p -> YearMonth.from(p.getFechaPago()).equals(mesActual))
                    .filter(p -> p.getFechaPago().getDayOfMonth() == dia)
                    .mapToDouble(p -> p.getMonto() != null ? p.getMonto().doubleValue() : 0.0)
                    .sum();
            datosLineaIngresos.put(String.valueOf(dia), montoDia);
        }
        String chartLineaIngresosBase64 = generarGraficoLineaBase64(datosLineaIngresos, "Linea de tiempo de ingresos (mes actual)", "Dia", "Monto");

        // Preparar contexto Thymeleaf
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("pagos", pagos);
        data.put("fechaEmision", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        data.put("reporteId", "RP-" + System.currentTimeMillis());
        data.put("totalRecaudado", String.format("$ %.2f", totalMonto));
        data.put("cantidadTransacciones", pagos.size());
        data.put("morososCount", morososCount);
        data.put("deudaTotal", String.format("$ %.2f", deudaTotal));
        data.put("ingresosPorCurso", ingresosPorCurso);
        data.put("detalleCuotas", detalleCuotasList); // Pasamos la nueva lista al contexto
        data.put("chartImage", chartBase64);
        data.put("chartComparativoMes", chartComparativoMesBase64);
        data.put("chartLineaIngresos", chartLineaIngresosBase64);
        data.put("estilos", cargarEstilos());
        data.put("mostrarFiscalEnHeader", true);
        agregarDatosBaseReporte(data);

        return generarReportePdfDesdePlantilla("reporte/reportePagos", data);
    }

    /**
     * Version HTML+CSS (Profesional) del reporte de ofertas
     */
    public ByteArrayInputStream generarReporteOfertasPDF(List<OfertaAcademica> ofertas, LocalDate fechaInicio, LocalDate fechaFin) {
        // --- KPIs ---
        long totalOfertas = ofertas.size();
        long ofertasEnCurso = ofertas.stream()
                .filter(o -> o.getEstado() == EstadoOferta.ENCURSO)
                .count();
        long totalInscritos = ofertas.stream().mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0).sum();
        
        // Ingresos Estimados (Inscritos * Costo)
        double ingresosEstimados = ofertas.stream()
                .mapToDouble(o -> {
                    long insc = o.getInscripciones() != null ? o.getInscripciones().size() : 0;
                    double cost = o.getCostoInscripcion() != null ? o.getCostoInscripcion() : 0.0;
                    return insc * cost;
                }).sum();

        // Calculo de ocupacion y alertas (Ofertas con cupos definidos)
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

        // Tasa de llenado global (% cupos ocupados vs cupos disponibles)
        long cuposTotales = ofertas.stream()
                .filter(o -> o.getCupos() != null && o.getCupos() < 2000000000)
                .mapToLong(OfertaAcademica::getCupos)
                .sum();
        long inscritosEnOfertasConCupo = ofertas.stream()
                .filter(o -> o.getCupos() != null && o.getCupos() < 2000000000)
                .mapToLong(o -> o.getInscripciones() != null ? o.getInscripciones().size() : 0)
                .sum();
        double tasaLlenadoGlobal = cuposTotales > 0 ? (inscritosEnOfertasConCupo * 100.0 / cuposTotales) : 0.0;

        // --- Grafico: Ofertas por Tipo ---
        Map<String, Number> datosGrafico = ofertas.stream()
            .collect(Collectors.groupingBy(
                o -> o.getTipoOferta() != null ? o.getTipoOferta() : "DESCONOCIDO", 
                Collectors.counting()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String chartBase64 = datosGrafico.isEmpty()
                ? null
                : generarGraficoTortaBase64(datosGrafico, "Distribucion por Tipo");

        // --- Comparativo por modalidad (inscripciones totales) ---
        Map<String, Number> comparativoModalidad = new LinkedHashMap<>();
        comparativoModalidad.put("PRESENCIAL", 0L);
        comparativoModalidad.put("VIRTUAL", 0L);
        comparativoModalidad.put("HIBRIDA", 0L);
        for (OfertaAcademica oferta : ofertas) {
            long insc = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
            if (oferta.getModalidad() != null) {
                String key = oferta.getModalidad().name();
                comparativoModalidad.put(key, ((Number) comparativoModalidad.getOrDefault(key, 0L)).longValue() + insc);
            }
        }
        long totalModalidad = comparativoModalidad.values().stream()
                .mapToLong(v -> v != null ? v.longValue() : 0L)
                .sum();
        String chartModalidadComparativo = totalModalidad <= 0
                ? null
                : generarGraficoBarrasHorizontalesBase64(
                        comparativoModalidad,
                        "Inscripciones por modalidad",
                        "Inscripciones",
                        "Modalidad"
                );

        // --- Tendencia temporal de inscripcion ---
        Map<LocalDate, Long> inscripcionesPorFecha = ofertas.stream()
                .filter(o -> o.getInscripciones() != null)
                .flatMap(o -> o.getInscripciones().stream())
                .filter(i -> i.getFechaInscripcion() != null)
                .filter(i -> (fechaInicio == null || !i.getFechaInscripcion().isBefore(fechaInicio))
                        && (fechaFin == null || !i.getFechaInscripcion().isAfter(fechaFin)))
                .collect(Collectors.groupingBy(Inscripciones::getFechaInscripcion, Collectors.counting()));
        Map<String, Number> tendenciaInscripciones = new LinkedHashMap<>();
        inscripcionesPorFecha.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> tendenciaInscripciones.put(e.getKey().format(REPORT_DATE_FORMAT), e.getValue()));
        String chartTendenciaInscripcion = tendenciaInscripciones.isEmpty()
                ? null
                : generarGraficoLineaBase64(
                        tendenciaInscripciones,
                        "Tendencia de inscripcion por fecha",
                        "Fecha",
                        "Inscripciones"
                );

        // % de inscripciones en la ultima semana antes del inicio (agregado)
        long totalInscripcionesConFecha = ofertas.stream()
                .filter(o -> o.getInscripciones() != null)
                .flatMap(o -> o.getInscripciones().stream())
                .filter(i -> i.getFechaInscripcion() != null)
                .count();
        long inscripcionesUltimaSemana = ofertas.stream()
                .filter(o -> o.getFechaInicio() != null && o.getInscripciones() != null)
                .flatMap(o -> o.getInscripciones().stream()
                        .filter(i -> i.getFechaInscripcion() != null)
                        .filter(i -> !i.getFechaInscripcion().isBefore(o.getFechaInicio().minusDays(7))
                                && !i.getFechaInscripcion().isAfter(o.getFechaInicio())))
                .count();
        double porcentajeUltimaSemana = totalInscripcionesConFecha > 0
                ? (inscripcionesUltimaSemana * 100.0 / totalInscripcionesConFecha)
                : 0.0;

        // --- Lista de espera / interes vs inscripcion / evolucion de interes ---
        List<Map<String, Object>> listaEsperaConsolidada = new java.util.ArrayList<>();
        List<Map<String, Object>> interesVsInscripcion = new java.util.ArrayList<>();
        List<Map<String, Object>> evolucionInteresPeriodo = new java.util.ArrayList<>();
        final boolean agruparSemanal = fechaInicio != null
                && fechaFin != null
                && java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin) <= 93;
        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (OfertaAcademica oferta : ofertas) {
            long insc = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
            List<NotificacionInscripcion> solicitudes = notificacionInscripcionRepository.findByOferta(oferta);
            long interesados = solicitudes.size();

            Map<String, Object> ivs = new HashMap<>();
            ivs.put("oferta", oferta.getNombre());
            ivs.put("interesados", interesados); // proxy de interes (solicitudes de notificacion)
            ivs.put("inscriptos", insc);
            ivs.put("conversion", interesados > 0 ? String.format(Locale.US, "%.1f%%", (insc * 100.0 / interesados)) : "-");
            interesVsInscripcion.add(ivs);

            Map<LocalDate, Long> porFechaSolicitud = solicitudes.stream()
                    .filter(n -> n.getFechaSolicitud() != null)
                    .collect(Collectors.groupingBy(n -> n.getFechaSolicitud().toLocalDate(), Collectors.counting()));
            if (!porFechaSolicitud.isEmpty()) {
                Map<LocalDate, Long> solicitudesPorPeriodo = porFechaSolicitud.entrySet().stream()
                        .collect(Collectors.groupingBy(
                                e -> {
                                    LocalDate f = e.getKey();
                                    if (agruparSemanal) {
                                        int delta = f.getDayOfWeek().getValue() - 1;
                                        return f.minusDays(delta);
                                    }
                                    return LocalDate.of(f.getYear(), f.getMonth(), 1);
                                },
                                TreeMap::new,
                                Collectors.summingLong(Map.Entry::getValue)
                        ));
                Long periodoAnterior = null;
                for (Map.Entry<LocalDate, Long> entry : solicitudesPorPeriodo.entrySet()) {
                    long actual = entry.getValue();
                    String variacion = "-";
                    if (periodoAnterior != null && periodoAnterior > 0) {
                        double var = ((actual - periodoAnterior) * 100.0) / periodoAnterior;
                        variacion = String.format(Locale.US, "%+.1f%%", var);
                    }
                    Map<String, Object> filaPeriodo = new HashMap<>();
                    filaPeriodo.put("oferta", oferta.getNombre());
                    filaPeriodo.put("periodo", agruparSemanal
                            ? entry.getKey().format(REPORT_DATE_FORMAT)
                            : entry.getKey().format(mesFormatter));
                    filaPeriodo.put("solicitudes", actual);
                    filaPeriodo.put("variacion", variacion);
                    evolucionInteresPeriodo.add(filaPeriodo);
                    periodoAnterior = actual;
                }
            }

            boolean cupoAgotado = oferta.getCupos() != null && oferta.getCupos() < 2000000000 && insc >= oferta.getCupos();
            if (cupoAgotado) {
                List<NotificacionInscripcion> pendientes = notificacionInscripcionRepository.findByOfertaAndNotificadoFalse(oferta);
                if (!pendientes.isEmpty()) {
                    String cupoTexto = (oferta.getCupos() == null || oferta.getCupos() >= 2000000000)
                            ? "Sin limite"
                            : String.valueOf(oferta.getCupos());
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("oferta", oferta.getNombre());
                    fila.put("cupo", cupoTexto);
                    fila.put("inscriptos", insc);
                    fila.put("enEspera", pendientes.size());
                    listaEsperaConsolidada.add(fila);
                }
            }
        }
        interesVsInscripcion.sort((a, b) -> Long.compare(
                ((Number) b.get("interesados")).longValue(),
                ((Number) a.get("interesados")).longValue()
        ));
        evolucionInteresPeriodo.sort((a, b) -> {
            int cmp = String.valueOf(a.get("oferta")).compareToIgnoreCase(String.valueOf(b.get("oferta")));
            if (cmp != 0) return cmp;
            return String.valueOf(a.get("periodo")).compareTo(String.valueOf(b.get("periodo")));
        });
        listaEsperaConsolidada.sort((a, b) -> Integer.compare(
                ((Number) b.get("enEspera")).intValue(),
                ((Number) a.get("enEspera")).intValue()
        ));

        // --- Inscripciones por Categoria ---
        Map<String, Long> inscripcionesPorCategoria = new LinkedHashMap<>();
        List<Categoria> categoriasSistema = categoriaRepository.findAll();
        for (Categoria categoria : categoriasSistema) {
            if (categoria != null && categoria.getNombre() != null && !categoria.getNombre().isBlank()) {
                inscripcionesPorCategoria.put(categoria.getNombre(), 0L);
            }
        }
        for (OfertaAcademica oferta : ofertas) {
            long insc = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
            List<Categoria> categorias = oferta.getCategorias();
            if (categorias == null || categorias.isEmpty()) {
                inscripcionesPorCategoria.merge("Sin categorias", insc, Long::sum);
                continue;
            }
            for (Categoria categoria : categorias) {
                String nombre = (categoria != null && categoria.getNombre() != null && !categoria.getNombre().isBlank())
                        ? categoria.getNombre()
                        : "Sin categoria";
                inscripcionesPorCategoria.merge(nombre, insc, Long::sum);
            }
        }

        List<Map<String, Object>> inscripcionesPorCategoriaList = inscripcionesPorCategoria.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return a.getKey().compareToIgnoreCase(b.getKey());
                })
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("categoria", e.getKey());
                    m.put("inscritos", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Number> inscripcionesPorCategoriaChart = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : inscripcionesPorCategoria.entrySet()) {
            inscripcionesPorCategoriaChart.put(entry.getKey(), entry.getValue());
        }
        long totalCategorias = inscripcionesPorCategoria.values().stream().mapToLong(Long::longValue).sum();
        String chartCategoriasBase64 = totalCategorias <= 0
                ? null
                : generarGraficoBarrasBase64(inscripcionesPorCategoriaChart, "Inscripciones por Categoria");

        // --- Contexto ---
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        // Periodo Text
        String periodoTexto = "Historico Completo";
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
        data.put("ofertasEnCurso", ofertasEnCurso);
        data.put("totalInscritos", totalInscritos);
        data.put("ofertasBajaOcupacion", ofertasConBajaOcupacion);
        data.put("ofertasLlenas", ofertasLlenas);
        data.put("tasaLlenadoGlobal", String.format(Locale.US, "%.1f%%", tasaLlenadoGlobal));
        data.put("ingresosEstimados", String.format("$ %.2f", ingresosEstimados));
        data.put("chartImage", chartBase64);
        data.put("chartModalidadComparativo", chartModalidadComparativo);
        data.put("chartTendenciaInscripcion", chartTendenciaInscripcion);
        data.put("porcentajeUltimaSemana", String.format(Locale.US, "%.1f%%", porcentajeUltimaSemana));
        data.put("estilos", cargarEstilos());
        List<Map<String, Object>> bajasAutomaticas = obtenerBajasPorAnalisis(ofertas);
        data.put("bajasAutomaticas", bajasAutomaticas);
        data.put("bajasAutomaticasCount", bajasAutomaticas.size());
        data.put("inscripcionesPorCategoria", inscripcionesPorCategoriaList);
        data.put("chartCategorias", chartCategoriasBase64);
        data.put("listaEsperaConsolidada", listaEsperaConsolidada);
        data.put("interesVsInscripcion", interesVsInscripcion);
        data.put("evolucionInteresPeriodo", evolucionInteresPeriodo);
        data.put("etiquetaPeriodoInteres", agruparSemanal ? "Semana" : "Mes");
        agregarDatosBaseReporte(data);

        return generarReportePdfDesdePlantilla("reporte/reporteOfertas", data);
    }

    /**
     * Version HTML+CSS (Profesional) del reporte de usuarios
     */
    @Transactional(readOnly = true)
    public ByteArrayInputStream generarReporteUsuariosPDF(List<Usuario> usuarios, LocalDate fechaInicio, LocalDate fechaFin, Integer inactividadDias) {
        long totalUsuariosReporte = usuarios.size();
        long totalUsuariosSistema = usuarioRepository.count();
        long usuariosActivos = usuarios.stream().filter(Usuario::isEstado).count();
        long usuariosInactivos = totalUsuariosReporte - usuariosActivos;

        int umbralInactividad = (inactividadDias != null && inactividadDias > 0) ? inactividadDias : 30;
        LocalDate hoy = LocalDate.now();

        long nuevosIngresos = usuarios.stream()
                .filter(u -> u.getFechaRegistro() != null)
                .filter(u -> fechaInicio == null || !u.getFechaRegistro().toLocalDate().isBefore(fechaInicio))
                .filter(u -> fechaFin == null || !u.getFechaRegistro().toLocalDate().isAfter(fechaFin))
                .count();

        Map<String, Long> rolesCount = usuarios.stream()
                .filter(u -> u.getRoles() != null)
                .flatMap(u -> u.getRoles().stream())
                .filter(r -> r != null && r.getNombre() != null && !r.getNombre().isBlank())
                .collect(Collectors.groupingBy(r -> r.getNombre(), Collectors.counting()));

        Map<String, Integer> ofertasActivasPorUsuario = new HashMap<>();
        Map<String, String> ultimoAccesoPorUsuario = new HashMap<>();
        Map<String, String> estadoInscripcionPorUsuario = new HashMap<>();
        Map<String, String> modalidadPreferidaPorUsuario = new HashMap<>();
        for (Usuario usuario : usuarios) {
            if (usuario != null && usuario.getId() != null) {
                int activas = inscripcionRepository.countByAlumnoIdAndEstadoInscripcionTrue(usuario.getId());
                String id = usuario.getId().toString();
                ofertasActivasPorUsuario.put(id, activas);

                LocalDateTime ultimoAcceso = obtenerUltimoAccesoUsuario(usuario.getId());
                ultimoAccesoPorUsuario.put(id,
                        ultimoAcceso != null ? ultimoAcceso.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-");

                estadoInscripcionPorUsuario.put(id, calcularEstadoInscripcionUsuario(usuario.getId(), activas));
                modalidadPreferidaPorUsuario.put(id, calcularModalidadPreferidaUsuario(usuario.getId()));
            }
        }

        long totalInscripcionesActivasUsuarios = ofertasActivasPorUsuario.values().stream().mapToLong(Integer::longValue).sum();
        double promedioInscripcionesPorUsuario = totalUsuariosReporte > 0
                ? (double) totalInscripcionesActivasUsuarios / totalUsuariosReporte
                : 0.0;
        long usuariosConInscripcionActiva = ofertasActivasPorUsuario.values().stream().filter(v -> v != null && v > 0).count();
        double porcentajeUsuariosConInscripcion = totalUsuariosReporte > 0
                ? (usuariosConInscripcionActiva * 100.0 / totalUsuariosReporte)
                : 0.0;

        long alumnosConInscripcion = usuarios.stream()
                .filter(u -> tieneRol(u, "ALUMNO"))
                .filter(u -> u.getId() != null && ofertasActivasPorUsuario.getOrDefault(u.getId().toString(), 0) > 0)
                .count();
        long alumnosSinActividadReciente = usuarios.stream()
                .filter(u -> tieneRol(u, "ALUMNO"))
                .filter(u -> u.getId() != null && ofertasActivasPorUsuario.getOrDefault(u.getId().toString(), 0) > 0)
                .filter(u -> {
                    LocalDateTime ultimo = obtenerUltimoAccesoUsuario(u.getId());
                    if (ultimo == null) return true;
                    return java.time.temporal.ChronoUnit.DAYS.between(ultimo.toLocalDate(), hoy) > umbralInactividad;
                })
                .count();
        double tasaAbandono = alumnosConInscripcion > 0 ? (alumnosSinActividadReciente * 100.0 / alumnosConInscripcion) : 0.0;
        double tasaRetencion = 100.0 - tasaAbandono;

        String rolPredominante = rolesCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("SIN_ROL");
        long cantidadRolPredominante = rolesCount.getOrDefault(rolPredominante, 0L);

        Map<String, Number> datosGraficoRol = new LinkedHashMap<>();
        rolesCount.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> datosGraficoRol.put(e.getKey(), e.getValue()));
        if (datosGraficoRol.isEmpty()) datosGraficoRol.put("Sin datos", 0);
        String chartRolesBase64 = generarGraficoTortaBase64(datosGraficoRol, "Distribucion de usuarios por rol");

        Map<String, Number> datosGraficoEstado = new LinkedHashMap<>();
        datosGraficoEstado.put("Activos", usuariosActivos);
        datosGraficoEstado.put("Inactivos", usuariosInactivos);
        String chartEstadoBase64 = generarGraficoTortaBase64(datosGraficoEstado, "Estado de usuarios filtrados");

        Map<String, Long> modalidadCount = modalidadPreferidaPorUsuario.values().stream()
                .filter(m -> m != null && !m.isBlank() && !"-".equals(m))
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()));
        Map<String, Number> datosGraficoModalidad = new LinkedHashMap<>();
        datosGraficoModalidad.put("PRESENCIAL", modalidadCount.getOrDefault("PRESENCIAL", 0L));
        datosGraficoModalidad.put("VIRTUAL", modalidadCount.getOrDefault("VIRTUAL", 0L));
        datosGraficoModalidad.put("HIBRIDA", modalidadCount.getOrDefault("HIBRIDA", 0L));
        String chartModalidadBase64 = generarGraficoBarrasHorizontalesBase64(
                datosGraficoModalidad,
                "Modalidad preferida por usuarios",
                "Cantidad de usuarios",
                "Modalidad"
        );

        long nuevosIngresosAnterior = 0;
        long usuariosActivosAnterior = 0;
        boolean mostrarComparacionPeriodo = false;
        String comparacionPeriodoTexto = "";
        String chartComparativaBase64 = null;
        if (fechaInicio != null && fechaFin != null && !fechaFin.isBefore(fechaInicio)) {
            mostrarComparacionPeriodo = true;
            String tipoComparacion = resolverTipoComparacion(null, fechaInicio, fechaFin);
            LocalDate[] periodoAnterior = calcularPeriodoAnterior(fechaInicio, fechaFin, tipoComparacion);
            LocalDate inicioAnterior = periodoAnterior[0];
            LocalDate finAnterior = periodoAnterior[1];

            List<Usuario> usuariosPrevios = usuarioRepository.findAll().stream()
                    .filter(u -> u.getFechaRegistro() != null)
                    .filter(u -> {
                        LocalDate f = u.getFechaRegistro().toLocalDate();
                        return !f.isBefore(inicioAnterior) && !f.isAfter(finAnterior);
                    })
                    .collect(Collectors.toList());
            nuevosIngresosAnterior = usuariosPrevios.size();
            usuariosActivosAnterior = usuariosPrevios.stream().filter(Usuario::isEstado).count();

            double variacionAltas = nuevosIngresosAnterior > 0
                    ? ((double) (nuevosIngresos - nuevosIngresosAnterior) / nuevosIngresosAnterior) * 100.0
                    : (nuevosIngresos > 0 ? 100.0 : 0.0);
            String etiquetaComparacion = switch (tipoComparacion) {
                case "semana" -> "semana anterior";
                case "mes" -> "mes anterior";
                case "anio" -> "anio anterior";
                default -> "periodo anterior equivalente";
            };
            comparacionPeriodoTexto = "Altas en periodo: " + nuevosIngresos
                    + " vs " + nuevosIngresosAnterior + " (" + String.format(Locale.US, "%+.1f%%", variacionAltas)
                    + ") respecto de " + etiquetaComparacion + ".";

            Map<String, Number> datosComparativa = new LinkedHashMap<>();
            datosComparativa.put("Altas actual", nuevosIngresos);
            datosComparativa.put("Altas anterior", nuevosIngresosAnterior);
            datosComparativa.put("Activos actual", usuariosActivos);
            datosComparativa.put("Activos anterior", usuariosActivosAnterior);
            chartComparativaBase64 = generarGraficoBarrasBase64(datosComparativa, "Comparativa periodo actual vs anterior");
        }

        String observacionesUsuariosBase = "Usuarios filtrados: " + totalUsuariosReporte + " de " + totalUsuariosSistema + ". "
                + "Rol predominante: " + rolPredominante + " (" + cantidadRolPredominante + "). "
                + "Inscripciones activas: " + totalInscripcionesActivasUsuarios + ".";

        Map<String, Object> payloadNarrativaUsuariosIA = new HashMap<>();
        payloadNarrativaUsuariosIA.put("totalUsuariosSistema", totalUsuariosSistema);
        payloadNarrativaUsuariosIA.put("totalUsuariosReporte", totalUsuariosReporte);
        payloadNarrativaUsuariosIA.put("usuariosActivos", usuariosActivos);
        payloadNarrativaUsuariosIA.put("usuariosInactivos", usuariosInactivos);
        payloadNarrativaUsuariosIA.put("nuevosIngresos", nuevosIngresos);
        payloadNarrativaUsuariosIA.put("rolesCount", rolesCount);
        payloadNarrativaUsuariosIA.put("totalInscripcionesActivasUsuarios", totalInscripcionesActivasUsuarios);
        payloadNarrativaUsuariosIA.put("promedioInscripcionesPorUsuario", String.format(Locale.US, "%.2f", promedioInscripcionesPorUsuario));
        payloadNarrativaUsuariosIA.put("usuariosConInscripcionActiva", usuariosConInscripcionActiva);
        payloadNarrativaUsuariosIA.put("porcentajeUsuariosConInscripcionActiva", String.format(Locale.US, "%.1f", porcentajeUsuariosConInscripcion));
        payloadNarrativaUsuariosIA.put("rolPredominante", rolPredominante);
        payloadNarrativaUsuariosIA.put("tasaRetencion", String.format(Locale.US, "%.1f", tasaRetencion));
        payloadNarrativaUsuariosIA.put("tasaAbandono", String.format(Locale.US, "%.1f", tasaAbandono));
        payloadNarrativaUsuariosIA.put("umbralInactividadDias", umbralInactividad);

        String observacionesUsuariosIA = null;
        try {
            observacionesUsuariosIA = chatServiceSimple.generarNarrativaReporteUsuarios(payloadNarrativaUsuariosIA);
        } catch (Exception ignored) {
        }
        String observacionesUsuarios = (observacionesUsuariosIA != null && !observacionesUsuariosIA.isBlank()
                && esNarrativaUsuariosConsistente(observacionesUsuariosIA, tasaAbandono, tasaRetencion))
                ? resumirObservacionCorta(observacionesUsuariosIA)
                : observacionesUsuariosBase;

        List<String> observacionesBreves = new java.util.ArrayList<>();
        observacionesBreves.add("Periodo: " + (fechaInicio != null && fechaFin != null ? fechaInicio.format(REPORT_DATE_FORMAT) + " al " + fechaFin.format(REPORT_DATE_FORMAT) : "historico"));
        observacionesBreves.add("Retencion " + String.format(Locale.US, "%.1f%%", tasaRetencion)
                + " | Abandono " + String.format(Locale.US, "%.1f%%", tasaAbandono)
                + " (inactividad > " + umbralInactividad + " dias).");
        if (mostrarComparacionPeriodo) {
            observacionesBreves.add(comparacionPeriodoTexto);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodoTexto = "Historico Completo";
        if (fechaInicio != null && fechaFin != null) {
            periodoTexto = fechaInicio.format(formatter) + " al " + fechaFin.format(formatter);
        } else if (fechaInicio != null) {
            periodoTexto = "Desde " + fechaInicio.format(formatter);
        } else if (fechaFin != null) {
            periodoTexto = "Hasta " + fechaFin.format(formatter);
        }

        String generadoPor = "Sistema";
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
             generadoPor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }

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
        data.put("altasPeriodo", nuevosIngresos);
        data.put("tasaRetencion", String.format(Locale.US, "%.1f%%", tasaRetencion));
        data.put("tasaAbandono", String.format(Locale.US, "%.1f%%", tasaAbandono));
        data.put("umbralInactividadDias", umbralInactividad);
        data.put("ofertasActivasPorUsuario", ofertasActivasPorUsuario);
        data.put("ultimoAccesoPorUsuario", ultimoAccesoPorUsuario);
        data.put("estadoInscripcionPorUsuario", estadoInscripcionPorUsuario);
        data.put("modalidadPreferidaPorUsuario", modalidadPreferidaPorUsuario);
        data.put("observacionesUsuarios", observacionesUsuarios);
        data.put("observacionesBreves", observacionesBreves);
        data.put("comparacionPeriodoUsuarios", comparacionPeriodoTexto);
        data.put("mostrarComparacionPeriodoUsuarios", mostrarComparacionPeriodo);
        data.put("chartImage", chartRolesBase64);
        data.put("chartRolesImage", chartRolesBase64);
        data.put("chartEstadoImage", chartEstadoBase64);
        data.put("chartModalidadImage", chartModalidadBase64);
        data.put("chartComparativaImage", chartComparativaBase64);
        data.put("estilos", cargarEstilos());
        agregarDatosBaseReporte(data);

        return generarReportePdfDesdePlantilla("reporte/reporteUsuarios", data);
    }

    private String resumirObservacionCorta(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String limpio = texto.replaceAll("\\s+", " ").trim();
        return limpio;
    }
public List<Usuario> filtrarUsuariosPorInactividad(List<Usuario> usuarios, Integer inactividadDias) {
        if (usuarios == null || inactividadDias == null || inactividadDias <= 0) return usuarios;
        LocalDate hoy = LocalDate.now();
        return usuarios.stream()
                .filter(u -> {
                    if (u == null || u.getId() == null) return false;
                    LocalDateTime ultimo = obtenerUltimoAccesoUsuario(u.getId());
                    if (ultimo == null) return true;
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(ultimo.toLocalDate(), hoy);
                    return dias > inactividadDias;
                })
                .collect(Collectors.toList());
    }

    private LocalDateTime obtenerUltimoAccesoUsuario(java.util.UUID usuarioId) {
        try {
            List<AuditLog> logs = auditLogRepository.findByUsuarioId(usuarioId);
            return logs.stream()
                    .filter(l -> l.isExito())
                    .filter(l -> l.getAccion() != null)
                    .filter(l -> {
                        String accion = l.getAccion().trim().toUpperCase(Locale.ROOT);
                        return "INICIO_SESION".equals(accion) || "LOGIN".equals(accion);
                    })
                    .filter(l -> l.getFecha() != null)
                    .map(l -> {
                        LocalDate d = l.getFecha().toLocalDate();
                        java.time.LocalTime t = l.getHora() != null ? l.getHora().toLocalTime() : java.time.LocalTime.MIN;
                        return LocalDateTime.of(d, t);
                    })
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean tieneRol(Usuario usuario, String rol) {
        if (usuario == null || usuario.getRoles() == null) return false;
        return usuario.getRoles().stream().anyMatch(r -> r != null && rol.equalsIgnoreCase(r.getNombre()));
    }

    private String calcularEstadoInscripcionUsuario(java.util.UUID usuarioId, int inscripcionesActivas) {
        if (inscripcionesActivas <= 0) return "SIN INSCRIPCION ACTIVA";
        try {
            List<com.example.demo.model.Cuota> cuotas = cuotaRepository.findByUsuarioId(usuarioId);
            boolean conMora = cuotas.stream().anyMatch(c ->
                    c.getEstado() == EstadoCuota.PENDIENTE
                            && c.getFechaVencimiento() != null
                            && c.getFechaVencimiento().isBefore(LocalDate.now()));
            return conMora ? "CON MORA" : "AL DIA";
        } catch (Exception e) {
            return "ACTIVA";
        }
    }

    private String calcularModalidadPreferidaUsuario(java.util.UUID usuarioId) {
        try {
            List<Inscripciones> inscripciones = inscripcionRepository.findAll().stream()
                    .filter(i -> i.getAlumno() != null && usuarioId.equals(i.getAlumno().getId()))
                    .filter(i -> i.getOferta() != null && i.getOferta().getModalidad() != null)
                    .collect(Collectors.toList());
            if (inscripciones.isEmpty()) return "-";
            return inscripciones.stream()
                    .collect(Collectors.groupingBy(i -> i.getOferta().getModalidad().name(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");
        } catch (Exception e) {
            return "-";
        }
    }

    private boolean esNarrativaUsuariosConsistente(String texto, double tasaAbandono, double tasaRetencion) {
        if (texto == null || texto.isBlank()) return false;
        String t = texto.toLowerCase(Locale.ROOT);

        if (t.contains("inactividad") && t.contains("usuarios registrados durante")) {
            return false;
        }
        if (tasaAbandono >= 50.0 && (t.contains("abandono baja") || t.contains("abandono muy baja") || t.contains("alta retencion"))) {
            return false;
        }
        if (tasaRetencion >= 70.0 && t.contains("retencion baja")) {
            return false;
        }
        if (tasaRetencion <= 40.0 && t.contains("alta retencion")) {
            return false;
        }
        return true;
    }

    public ByteArrayInputStream generarReporteUsuariosExcel(List<Usuario> usuarios) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Usuarios");

            String[] columns = {"ID", "DNI", "Nombre", "Apellido", "Correo", "Telefono", "Ofertas Activas", "Roles", "Estado", "Fecha Registro"};
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

            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Usuario usuario : usuarios) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(usuario.getId() != null ? usuario.getId().toString() : "");
                row.createCell(1).setCellValue(usuario.getDni() != null ? usuario.getDni() : "");
                row.createCell(2).setCellValue(usuario.getNombre() != null ? usuario.getNombre() : "");
                row.createCell(3).setCellValue(usuario.getApellido() != null ? usuario.getApellido() : "");
                row.createCell(4).setCellValue(usuario.getCorreo() != null ? usuario.getCorreo() : "");
                row.createCell(5).setCellValue(usuario.getNumTelefono() != null ? usuario.getNumTelefono() : "");
                int ofertasActivas = (usuario.getId() != null)
                    ? inscripcionRepository.countByAlumnoIdAndEstadoInscripcionTrue(usuario.getId())
                    : 0;
                row.createCell(6).setCellValue(ofertasActivas);

                String roles = usuario.getRoles() != null && !usuario.getRoles().isEmpty()
                    ? usuario.getRoles().stream().map(r -> r.getNombre()).collect(Collectors.joining(", "))
                    : "";
                row.createCell(7).setCellValue(roles);
                row.createCell(8).setCellValue(usuario.isEstado() ? "ACTIVO" : "INACTIVO");
                row.createCell(9).setCellValue(usuario.getFechaRegistro() != null ? usuario.getFechaRegistro().format(formatter) : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
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

    private Color parseHexColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        String h = hex.trim().replace("#", "");
        if (h.length() != 6) return fallback;
        try {
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Color bg, Color fg) {
        com.lowagie.text.Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, fg);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        addBodyCell(table, text, font, Element.ALIGN_LEFT);
    }

    private void addBodyCell(PdfPTable table, String text, com.lowagie.text.Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, com.lowagie.text.Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, font));
        cellLabel.setBorder(0);
        cellLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellLabel.setPadding(4);
        PdfPCell cellValue = new PdfPCell(new Phrase(value, font));
        cellValue.setBorder(0);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellValue.setPadding(4);
        table.addCell(cellLabel);
        table.addCell(cellValue);
    }

    private String formatearMetodoPago(String tipoPago) {
        if (tipoPago == null || tipoPago.isBlank()) return "N/A";
        String t = tipoPago.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "account_money" -> "Transferencia Mercado Pago";
            case "credit_card" -> "Tarjeta de Credito";
            case "debit_card" -> "Tarjeta de Debito";
            case "bank_transfer" -> "Transferencia Bancaria";
            case "atm" -> "Pago por Cajero";
            default -> tipoPago;
        };
    }

    private String formatearMontoComa(java.math.BigDecimal monto) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String valor = nf.format(monto != null ? monto : java.math.BigDecimal.ZERO);
        return "$ " + valor;
    }
}

