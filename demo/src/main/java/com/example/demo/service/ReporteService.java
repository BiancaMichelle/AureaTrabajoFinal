package com.example.demo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.OfertaAcademica;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;

@Service
public class ReporteService {

    @Autowired
    private OfertaAcademicaRepository ofertaRepository;

    public List<OfertaAcademica> filtrarOfertas(String nombre, String estado, Long categoriaId, LocalDate fechaInicio, LocalDate fechaFin) {
        List<OfertaAcademica> todas = ofertaRepository.findAll();
        
        return todas.stream()
            .filter(o -> nombre == null || nombre.isEmpty() || o.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .filter(o -> estado == null || estado.isEmpty() || (o.getEstado() != null && o.getEstado().name().equalsIgnoreCase(estado)))
            .filter(o -> categoriaId == null || o.getCategorias().stream().anyMatch(c -> c.getIdCategoria().equals(categoriaId)))
            .filter(o -> fechaInicio == null || (o.getFechaInicio() != null && !o.getFechaInicio().isBefore(fechaInicio)))
            .filter(o -> fechaFin == null || (o.getFechaFin() != null && !o.getFechaFin().isAfter(fechaFin)))
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
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Clase interna para manejar eventos de página (Encabezado y Pie de página)
            class HeaderFooter extends PdfPageEventHelper {
                PdfTemplate total;
                BaseFont bf;

                @Override
                public void onOpenDocument(PdfWriter writer, Document document) {
                    try {
                        bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                        total = writer.getDirectContent().createTemplate(50, 50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        PdfContentByte cb = writer.getDirectContent();
                        float pageWidth = document.getPageSize().getWidth();

                        // Encabezado: "Aurea" centrado
                        Phrase header = new Phrase("Aurea", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, header, pageWidth / 2, document.top() + 20, 0);

                        // Pie de página: "Página x de "
                        String text = "Página " + writer.getPageNumber() + " de ";
                        float textSize = bf.getWidthPoint(text, 9);
                        float x = pageWidth / 2;
                        float y = document.bottom() - 20;

                        cb.beginText();
                        cb.setFontAndSize(bf, 9);
                        cb.setTextMatrix(x - textSize / 2, y);
                        cb.showText(text);
                        cb.endText();

                        // Añadir plantilla para el número total de páginas
                        cb.addTemplate(total, x - textSize / 2 + textSize, y);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCloseDocument(PdfWriter writer, Document document) {
                    try {
                        total.beginText();
                        total.setFontAndSize(bf, 9);
                        total.setTextMatrix(0, 0);
                        total.showText(String.valueOf(writer.getPageNumber() - 1));
                        total.endText();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Asignar el evento al writer
            writer.setPageEvent(new HeaderFooter());

            document.open();

            // Título
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Ofertas Académicas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            document.add(new Paragraph("Fecha de generación: " + LocalDate.now()));
            document.add(Chunk.NEWLINE);

            // Tabla
            PdfPTable table = new PdfPTable(11);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 4, 2, 2, 2, 2, 2, 1, 1, 2, 2});

            String[] headers = {"ID", "Nombre", "Tipo", "Estado", "Inicio", "Fin", "Costo", "Insc.", "Act.", "Aband. %", "Ingresos"};
            
            for (String header : headers) {
                PdfPCell hcell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
                hcell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(hcell);
            }

            for (OfertaAcademica oferta : ofertas) {
                int totalInscriptos = oferta.getInscripciones() != null ? oferta.getInscripciones().size() : 0;
                long activos = oferta.getInscripciones() != null ? oferta.getInscripciones().stream().filter(i -> Boolean.TRUE.equals(i.getEstadoInscripcion())).count() : 0;
                double abandono = totalInscriptos > 0 ? ((double)(totalInscriptos - activos) / totalInscriptos) * 100 : 0.0;
                double ingresos = totalInscriptos * (oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0);

                table.addCell(new Phrase(String.valueOf(oferta.getIdOferta()), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(oferta.getNombre(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(oferta.getTipoOferta(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(oferta.getEstado() != null ? oferta.getEstado().name() : "N/A", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(oferta.getFechaInicio() != null ? oferta.getFechaInicio().toString() : "", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(oferta.getFechaFin() != null ? oferta.getFechaFin().toString() : "", FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.valueOf(oferta.getCostoInscripcion() != null ? oferta.getCostoInscripcion() : 0.0), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.valueOf(totalInscriptos), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.valueOf(activos), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.format("%.1f", abandono), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Phrase(String.format("%.1f", ingresos), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            }

            document.add(table);
            document.close();

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}