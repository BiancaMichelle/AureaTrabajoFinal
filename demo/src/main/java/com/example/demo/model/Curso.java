package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

public class Curso extends OfertaAcademica {
    private List<Horario> horarios;
    private String temario;
    private List<Docente> docentes;
    private List<OfertaAcademica> requisitos;
    private double costoCuota;
    private double costoMora;
    private int nrCuotas;
    private LocalDate vencimientoCuota;
}
