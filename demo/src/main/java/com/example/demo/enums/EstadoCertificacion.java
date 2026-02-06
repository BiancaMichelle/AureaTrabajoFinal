package com.example.demo.enums;

public enum EstadoCertificacion {
    PENDIENTE,           // Alumno inscrito, oferta en curso
    PROPUESTA,           // Sistema propone para certificación (automático)
    APROBADO_DOCENTE,    // Docente aprobó para certificación
    RECHAZADO_DOCENTE,   // Docente rechazó (no cumple requisitos adicionales)
    CERTIFICADO_EMITIDO, // Certificado generado y enviado
    NO_APLICA            // No cumple criterios mínimos
}
