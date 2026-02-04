package com.example.demo.enums;

public enum EstadoOferta {
    ACTIVA,
    DE_BAJA,
    ENCURSO,
    FINALIZADA,      // Oferta terminó pero notas NO cerradas
    CERRADA          // Notas cerradas, certificados emitidos - INMUTABLE
}
