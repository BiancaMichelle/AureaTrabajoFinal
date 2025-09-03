package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

public class Formacion extends OfertaAcademica {
    private String plan;
    private List<Docente> docentes;
    private double costoCuota;
    private double costoMora;
    private int nrCuotas;
    private LocalDate vencimientoCuota;
}
