package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.enums.Dias;
import com.example.demo.model.Docente;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.service.DisponibilidadDocenteService.BloqueHorario;
import com.example.demo.service.GeneradorHorariosService.HorarioAsignado;
import com.example.demo.service.GeneradorHorariosService.PropuestaHorario;

@ExtendWith(MockitoExtension.class)
public class GeneradorHorariosServiceTest {

    @Mock
    private DisponibilidadDocenteService disponibilidadService;

    @InjectMocks
    private GeneradorHorariosService generadorService;

    private Docente docente;
    private OfertaAcademica oferta;

    @BeforeEach
    public void setup() {
        docente = new Docente();
        oferta = new OfertaAcademica();
    }

    @Test
    public void testGenerarPropuestasConMaxHorasYPin() {
        // Disponibilidad: Lunes 7-12 (5h), Martes 7-12 (5h)
        Map<Dias, List<BloqueHorario>> horariosLibres = new HashMap<>();
        
        List<BloqueHorario> lunes = new ArrayList<>();
        lunes.add(new BloqueHorario(Dias.LUNES, Time.valueOf("07:00:00"), Time.valueOf("12:00:00")));
        horariosLibres.put(Dias.LUNES, lunes);

        List<BloqueHorario> martes = new ArrayList<>();
        martes.add(new BloqueHorario(Dias.MARTES, Time.valueOf("07:00:00"), Time.valueOf("12:00:00")));
        horariosLibres.put(Dias.MARTES, martes);

        when(disponibilidadService.calcularHorariosLibresSemana(any(Docente.class))).thenReturn(horariosLibres);
        when(disponibilidadService.calcularCargaHorariaSemanal(any(Docente.class))).thenReturn(10.0);

        // Caso: Queremos 6 horas totales.
        // Restricción: Máximo 3 horas por día.
        // Pin: Lunes 7:00-9:00 (2h)
        // Faltan: 4h.
        // Capacidad restante Lunes (con max 3): 1h.
        // Capacidad restante Martes (con max 3): 3h.
        // Total capacidad: 4h. Justo para cubrir la demanda.
        
        List<HorarioAsignado> pinned = new ArrayList<>();
        pinned.add(new HorarioAsignado(Dias.LUNES, Time.valueOf("07:00:00"), Time.valueOf("09:00:00"))); // 2h

        List<PropuestaHorario> propuestas = generadorService.generarPropuestas(
            oferta, 
            docente, 
            6.0, 
            3, // Max 3h/día
            pinned, 
            false
        );

        assertFalse(propuestas.isEmpty(), "Debería generar al menos una propuesta");

        PropuestaHorario p = propuestas.get(0);
        assertEquals(6.0, p.getTotalHorasSemana(), 0.01, "Debe sumar 6 horas");

        // Verificar distribución
        Map<Dias, Double> horasPorDia = new HashMap<>();
        for (HorarioAsignado h : p.getHorarios()) {
            horasPorDia.merge(h.getDia(), h.getDuracionHoras(), Double::sum);
        }

        // Lunes: 2h (pinned) + 1h (relleno/estrategia) = 3h total
        assertEquals(3.0, horasPorDia.get(Dias.LUNES), 0.01, "Lunes debe tener 3h (2 fijas + 1 nueva)");
        
        // Martes: 3h (estrategia)
        assertEquals(3.0, horasPorDia.get(Dias.MARTES), 0.01, "Martes debe tener 3h");
    }

    @Test
    public void testRellenoGranular() {
        // Disponibilidad muy fragmentada o justa
        // Lunes 7-8 (1h), Martes 7-8 (1h), Miercoles 7-8 (1h), Jueves 7-8 (1h)
        // Requerido: 3h.
        // Min horas clase es 1h (por defecto/constante).
        // Estrategias normales (Equilibrada) intentarán coger de L, M, X.
        
        // Vamos a poner un caso donde se necesite "relleno" o bloques < MIN_HORAS_CLASE si fuera 2h.
        // Como bajé MIN_HORAS_CLASE a 1h, es más fácil.
        // Probemos con bloques de 1.5h y requerimiento de 4h.
        // Lunes 7-8:30 (1.5h), Martes 7-8:30 (1.5h), Miercoles 7-8:30 (1.5h).
        // Necesitamos 4h.
        // 1.5 + 1.5 + 1 = 4. (Uno quedará con 0.5 libre).

        Map<Dias, List<BloqueHorario>> horariosLibres = new HashMap<>();
        horariosLibres.put(Dias.LUNES, Collections.singletonList(new BloqueHorario(Dias.LUNES, Time.valueOf("07:00:00"), Time.valueOf("08:30:00"))));
        horariosLibres.put(Dias.MARTES, Collections.singletonList(new BloqueHorario(Dias.MARTES, Time.valueOf("07:00:00"), Time.valueOf("08:30:00"))));
        horariosLibres.put(Dias.MIERCOLES, Collections.singletonList(new BloqueHorario(Dias.MIERCOLES, Time.valueOf("07:00:00"), Time.valueOf("08:30:00"))));

        when(disponibilidadService.calcularHorariosLibresSemana(any(Docente.class))).thenReturn(horariosLibres);
        when(disponibilidadService.calcularCargaHorariaSemanal(any(Docente.class))).thenReturn(0.0);

        List<PropuestaHorario> propuestas = generadorService.generarPropuestas(
            oferta, 
            docente, 
            4.0, 
            4, 
            null, 
            false
        );

        assertFalse(propuestas.isEmpty());
        PropuestaHorario p = propuestas.get(0);
        assertEquals(4.0, p.getTotalHorasSemana(), 0.01);
    }
}
