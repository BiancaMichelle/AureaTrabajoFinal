package com.example.demo.service;

import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.enums.Dias;
import com.example.demo.model.Docente;
import com.example.demo.model.Horario;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.service.DisponibilidadDocenteService.BloqueHorario;

/**
 * Servicio para generar automáticamente propuestas de horarios para ofertas académicas
 * basándose en la disponibilidad de los docentes asignados.
 */
@Service
public class GeneradorHorariosService {
    
    @Autowired
    private DisponibilidadDocenteService disponibilidadService;
    
    // Configuración de límites
    private static final int MAX_HORAS_DIARIAS_DEFAULT = 4;
    private static final int MIN_HORAS_CLASE = 1; // Cambiado a 1h según requerimiento
    private static final int MAX_PROPUESTAS = 3;
    
    /**
     * Genera propuestas automáticas de horarios para una oferta académica.
     * Método simplificado que usa valores por defecto.
     */
    public List<PropuestaHorario> generarPropuestas(OfertaAcademica oferta, Docente docente, double horasSemanalesRequeridas) {
        return generarPropuestas(oferta, docente, horasSemanalesRequeridas, MAX_HORAS_DIARIAS_DEFAULT, null, false);
    }

    /**
     * Genera propuestas con opciones avanzadas de control.
     * 
     * @param oferta La oferta académica
     * @param docente El docente asignado
     * @param horasSemanalesRequeridas Horas totales necesarias
     * @param maxHorasDiarias Máximo de horas permitidas por día
     * @param horariosFijados Lista de horarios que se deben mantener (pinned)
     * @param buscarAlternativas Si es true, intenta generar combinaciones diferentes a las standard
     * @return Lista de propuestas
     */
    public List<PropuestaHorario> generarPropuestas(OfertaAcademica oferta, Docente docente, 
                                                   double horasSemanalesRequeridas,
                                                   int maxHorasDiarias,
                                                   List<HorarioAsignado> horariosFijados,
                                                   boolean buscarAlternativas) {
        
        if (docente == null) {
            throw new IllegalArgumentException("El docente es obligatorio");
        }
        
        if (horasSemanalesRequeridas <= 0) {
            throw new IllegalArgumentException("Las horas semanales requeridas deben ser mayores a 0");
        }

        // Validación de horarios fijados
        double horasYaAsignadas = 0;
        Map<Dias, Double> consumoPorDiaFijado = new HashMap<>();

        if (horariosFijados != null) {
            for (HorarioAsignado h : horariosFijados) {
                horasYaAsignadas += h.getDuracionHoras();
                consumoPorDiaFijado.merge(h.getDia(), h.getDuracionHoras(), Double::sum);
            }
        }

        if (horasYaAsignadas > horasSemanalesRequeridas) {
             throw new IllegalArgumentException("Las horas fijadas superan las horas requeridas");
        }

        double horasRestantes = horasSemanalesRequeridas - horasYaAsignadas;
        
        // Si ya cubrimos todo con los fijos, devolver una única propuesta con ellos
        if (horasRestantes <= 0.01) {
            PropuestaHorario p = new PropuestaHorario("Horario Fijado Completo");
            p.setDescripcion("Propuesta basada exclusivamente en los horarios fijados por el usuario");
            if (horariosFijados != null) horariosFijados.forEach(p::agregarHorario);
            p.calcularMetricas(disponibilidadService.calcularCargaHorariaSemanal(docente));
            return Arrays.asList(p);
        }

        List<PropuestaHorario> propuestas = new ArrayList<>();
        
        // Obtener bloques horarios libres del docente
        Map<Dias, List<BloqueHorario>> horariosLibresTotal = disponibilidadService.calcularHorariosLibresSemana(docente);
        
        // Restar los horarios fijados de la disponibilidad "libre" para no solapar
        Map<Dias, List<BloqueHorario>> disponibilidadNeta = restarHorariosFijados(horariosLibresTotal, horariosFijados);

        // Filtrar días con al menos 1h disponible (MIN_HORAS_CLASE)
        Map<Dias, List<BloqueHorario>> diasViables = filtrarDiasViables(disponibilidadNeta);
        
        if (diasViables.isEmpty() && horasRestantes > 0) {
             // Si no hay días viables estándar, intentamos con lo que haya si es posible (relleno puro)
             // Pero si es muy poco probable, devolvemos vacío para indicar fallo
             return new ArrayList<>(); 
        }
        
        // Ajustamos maxHorasDiarias para las estrategias (considerando lo ya fijado)
        // Las estrategias deben saber que en el día X ya gastamos Y horas
        
        // Estrategia 1: Distribución equilibrada
        PropuestaHorario propuesta1 = generarPropuestaEquilibrada(diasViables, disponibilidadNeta, horasRestantes, 
                                                                  "Distribución Equilibrada", maxHorasDiarias, consumoPorDiaFijado, buscarAlternativas);
        if (propuesta1 != null) {
            // Agregar los fijos
            if (horariosFijados != null) horariosFijados.forEach(propuesta1::agregarHorario);
            propuestas.add(propuesta1);
        }
        
        // Estrategia 2: Concentrada
        PropuestaHorario propuesta2 = generarPropuestaConcentrada(diasViables, disponibilidadNeta, horasRestantes, 
                                                                  "Concentrada", maxHorasDiarias, consumoPorDiaFijado, buscarAlternativas);
        if (propuesta2 != null) {
            if (horariosFijados != null) horariosFijados.forEach(propuesta2::agregarHorario);
            if (!esEquivalente(propuesta2, propuestas)) {
                propuestas.add(propuesta2);
            }
        }
        
        // Estrategia 3: Distribuida
        PropuestaHorario propuesta3 = generarPropuestaDistribuida(diasViables, disponibilidadNeta, horasRestantes, 
                                                                  "Distribuida", maxHorasDiarias, consumoPorDiaFijado, buscarAlternativas);
        if (propuesta3 != null) {
            if (horariosFijados != null) horariosFijados.forEach(propuesta3::agregarHorario);
            if (!esEquivalente(propuesta3, propuestas)) {
                propuestas.add(propuesta3);
            }
        }
        
        // Calcular métricas
        double cargaActual = disponibilidadService.calcularCargaHorariaSemanal(docente);
        for (PropuestaHorario propuesta : propuestas) {
            propuesta.calcularMetricas(cargaActual);
        }
        
        // Ordenar por score
        propuestas.sort(Comparator.comparingDouble(PropuestaHorario::getScore).reversed());
        
        return propuestas.stream().limit(MAX_PROPUESTAS).collect(Collectors.toList());
    }

    /**
     * Genera propuestas mixtas cuando hay varios docentes.
     * Primero intenta usar intersecciones (todos disponibles), luego completa con unión (cualquiera disponible).
     */
    public List<PropuestaHorario> generarPropuestasMulti(OfertaAcademica oferta,
                                                         List<Docente> docentes,
                                                         double horasSemanalesRequeridas,
                                                         int maxHorasDiarias,
                                                         List<HorarioAsignado> horariosFijados,
                                                         boolean buscarAlternativas) {
        if (docentes == null || docentes.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos un docente");
        }
        if (docentes.size() == 1) {
            return generarPropuestas(oferta, docentes.get(0), horasSemanalesRequeridas, maxHorasDiarias, horariosFijados, buscarAlternativas);
        }

        // Consumo fijo por docente/día
        Map<Docente, Map<Dias, Double>> consumoFijo = new HashMap<>();
        double horasYaAsignadas = 0.0;
        for (Docente d : docentes) {
            consumoFijo.put(d, new HashMap<>());
        }

        if (horariosFijados != null) {
            for (HorarioAsignado h : horariosFijados) {
                horasYaAsignadas += h.getDuracionHoras();
                // Si tiene docenteId, aplicar a ese docente, si no, aplicar a todos
                if (h.getDocenteId() != null && !h.getDocenteId().isBlank()) {
                    Docente match = docentes.stream()
                        .filter(d -> d.getId() != null && d.getId().toString().equals(h.getDocenteId()))
                        .findFirst().orElse(null);
                    if (match != null) {
                        consumoFijo.get(match).merge(h.getDia(), h.getDuracionHoras(), Double::sum);
                    }
                } else {
                    for (Docente d : docentes) {
                        consumoFijo.get(d).merge(h.getDia(), h.getDuracionHoras(), Double::sum);
                    }
                }
            }
        }

        double horasRestantes = horasSemanalesRequeridas - horasYaAsignadas;

        PropuestaHorario propuesta = new PropuestaHorario("Mixta");
        propuesta.setDescripcion("Intersección + unión entre docentes");
        if (horariosFijados != null) horariosFijados.forEach(propuesta::agregarHorario);

        if (horasRestantes <= 0.01) {
            return Arrays.asList(propuesta);
        }

        // Disponibilidad neta por docente
        Map<Docente, Map<Dias, List<BloqueHorario>>> disponibilidadNeta = new HashMap<>();
        for (Docente d : docentes) {
            Map<Dias, List<BloqueHorario>> libres = disponibilidadService.calcularHorariosLibresSemana(d);
            List<HorarioAsignado> fijosDoc = horariosFijados == null ? new ArrayList<>() :
                horariosFijados.stream()
                    .filter(h -> h.getDocenteId() == null || h.getDocenteId().isBlank()
                                 || (d.getId() != null && d.getId().toString().equals(h.getDocenteId())))
                    .collect(Collectors.toList());
            disponibilidadNeta.put(d, restarHorariosFijados(libres, fijosDoc));
        }

        // Intersección de bloques (todos disponibles)
        Map<Dias, List<BloqueHorario>> interseccion = new HashMap<>();
        for (Dias dia : Dias.values()) {
            List<BloqueHorario> inter = null;
            for (Docente d : docentes) {
                List<BloqueHorario> bloques = disponibilidadNeta.get(d).getOrDefault(dia, new ArrayList<>());
                if (inter == null) {
                    inter = new ArrayList<>(bloques);
                } else {
                    inter = intersectarBloques(inter, bloques);
                }
            }
            if (inter != null && !inter.isEmpty()) {
                interseccion.put(dia, inter);
            }
        }

        // Orden docentes (round-robin)
        List<Docente> ordenDocentes = new ArrayList<>(docentes);
        if (buscarAlternativas) java.util.Collections.shuffle(ordenDocentes);

        // Consumir intersección con round-robin
        for (Dias dia : interseccion.keySet()) {
            for (BloqueHorario b : interseccion.get(dia)) {
                if (horasRestantes <= 0.01) break;
                double capMin = Double.MAX_VALUE;
                for (Docente d : ordenDocentes) {
                    double consumido = consumoFijo.get(d).getOrDefault(dia, 0.0);
                    double cap = maxHorasDiarias - consumido;
                    capMin = Math.min(capMin, cap);
                }
                if (capMin < MIN_HORAS_CLASE) continue;
                double usar = Math.min(Math.min(b.getDuracionHoras(), capMin), horasRestantes);
                if (usar < MIN_HORAS_CLASE) continue;
                HorarioAsignado h = crearHorario(b, usar);
                h.setDocentesInfo(ordenDocentes);
                propuesta.agregarHorario(h);
                for (Docente d : ordenDocentes) {
                    consumoFijo.get(d).merge(dia, usar, Double::sum);
                }
                horasRestantes -= usar;
            }
        }

        // Completar con unión (cualquier docente)
        if (horasRestantes > 0.01) {
            for (Docente d : ordenDocentes) {
                Map<Dias, List<BloqueHorario>> disp = disponibilidadNeta.get(d);
                for (Dias dia : disp.keySet()) {
                    for (BloqueHorario b : disp.get(dia)) {
                        if (horasRestantes <= 0.01) break;
                        double consumido = consumoFijo.get(d).getOrDefault(dia, 0.0);
                        double cap = maxHorasDiarias - consumido;
                        if (cap < MIN_HORAS_CLASE) continue;
                        double usar = Math.min(Math.min(b.getDuracionHoras(), cap), horasRestantes);
                        if (usar < MIN_HORAS_CLASE) continue;
                        HorarioAsignado h = crearHorario(b, usar, d);
                        propuesta.agregarHorario(h);
                        consumoFijo.get(d).merge(dia, usar, Double::sum);
                        horasRestantes -= usar;
                    }
                }
            }
        }

        if (propuesta.getTotalHorasSemana() < MIN_HORAS_CLASE) {
            return new ArrayList<>();
        }
        if (horasRestantes > 0.01) {
            propuesta.setDescripcion(propuesta.getDescripcion() + " (parcial)");
        }

        propuesta.ordenarHorarios();
        propuesta.calcularMetricas(docentes.stream()
            .mapToDouble(d -> disponibilidadService.calcularCargaHorariaSemanal(d)).sum());

        return Arrays.asList(propuesta);
    }

    private List<BloqueHorario> intersectarBloques(List<BloqueHorario> a, List<BloqueHorario> b) {
        List<BloqueHorario> res = new ArrayList<>();
        for (BloqueHorario ba : a) {
            for (BloqueHorario bb : b) {
                if (ba.getDia() != bb.getDia()) continue;
                LocalTime ini = ba.getHoraInicio().toLocalTime().isAfter(bb.getHoraInicio().toLocalTime())
                    ? ba.getHoraInicio().toLocalTime() : bb.getHoraInicio().toLocalTime();
                LocalTime fin = ba.getHoraFin().toLocalTime().isBefore(bb.getHoraFin().toLocalTime())
                    ? ba.getHoraFin().toLocalTime() : bb.getHoraFin().toLocalTime();
                if (fin.isAfter(ini)) {
                    res.add(new BloqueHorario(ba.getDia(), Time.valueOf(ini), Time.valueOf(fin)));
                }
            }
        }
        return res;
    }

    private boolean esEquivalente(PropuestaHorario p, List<PropuestaHorario> lista) {
        return lista.stream().anyMatch(existing -> existing.esEquivalenteA(p));
    }

    /**
     * Estrategia 1: Distribución equilibrada con días alternados
     */
    private PropuestaHorario generarPropuestaEquilibrada(Map<Dias, List<BloqueHorario>> diasViables, 
                                                        Map<Dias, List<BloqueHorario>> todosHorarios,
                                                        double horasRequeridas, String nombre,
                                                        int maxHorasDiarias, Map<Dias, Double> consumoFijo,
                                                        boolean shuffle) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases distribuidas en días alternados para mejor balance");
        
        List<Dias> diasPreferidos = new ArrayList<>(Arrays.asList(Dias.LUNES, Dias.MIERCOLES, Dias.VIERNES));
        if (shuffle) {
             java.util.Collections.rotate(diasPreferidos, 1); // Rotación simple para variar
        }

        double horasAsignadas = 0;
        
        for (Dias dia : diasPreferidos) {
            if (!diasViables.containsKey(dia)) continue;
            if (horasAsignadas >= horasRequeridas) break;
            
            double consumido = consumoFijo.getOrDefault(dia, 0.0);
            double disponibleDia = maxHorasDiarias - consumido;
            if (disponibleDia < MIN_HORAS_CLASE) continue;

            List<BloqueHorario> bloques = diasViables.get(dia);
            double falta = horasRequeridas - horasAsignadas;
            double aPedir = Math.min(disponibleDia, falta);
            
            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, aPedir);
            
            if (mejorBloque != null) {
                // Aseguramos no pasarnos del bloque ni del tope diario
                double duracionBloque = mejorBloque.getDuracionHoras();
                double usarReal = Math.min(duracionBloque, aPedir);
                
                HorarioAsignado horario = crearHorario(mejorBloque, usarReal);
                propuesta.agregarHorario(horario);
                horasAsignadas += usarReal;
            }
        }
        
        // Segunda pasada: otros días
        if (horasAsignadas < horasRequeridas) {
            List<Dias> otrosDias = new ArrayList<>(diasViables.keySet());
            if (shuffle) java.util.Collections.shuffle(otrosDias);

            for (Dias dia : otrosDias) {
                if (diasPreferidos.contains(dia)) continue;
                if (horasAsignadas >= horasRequeridas) break;

                double consumido = consumoFijo.getOrDefault(dia, 0.0);
                double disponibleDia = maxHorasDiarias - consumido;
                if (disponibleDia < MIN_HORAS_CLASE) continue;
                
                List<BloqueHorario> bloques = diasViables.get(dia);
                double falta = horasRequeridas - horasAsignadas;
                double aPedir = Math.min(disponibleDia, falta);

                BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, aPedir);
                
                if (mejorBloque != null) {
                    double duracionBloque = mejorBloque.getDuracionHoras();
                    double usarReal = Math.min(duracionBloque, aPedir);

                    HorarioAsignado horario = crearHorario(mejorBloque, usarReal);
                    propuesta.agregarHorario(horario);
                    horasAsignadas += usarReal;
                }
            }
        }

        // Relleno granular si falta
        if (horasAsignadas < horasRequeridas) {
            double faltante = horasRequeridas - horasAsignadas;
            boolean completado = tryFillWithSmallerBlocks(propuesta, todosHorarios, faltante, diasPreferidos, maxHorasDiarias, consumoFijo);
            if (!completado) {
                propuesta.setDescripcion(propuesta.getDescripcion() + " (parcial)");
            }
        }
        if (propuesta.getTotalHorasSemana() < MIN_HORAS_CLASE) return null;
        return propuesta;
    }
    
    /**
     * Estrategia 2: Concentrada (menos días, más horas por día)
     */
    private PropuestaHorario generarPropuestaConcentrada(Map<Dias, List<BloqueHorario>> diasViables, 
                                                        Map<Dias, List<BloqueHorario>> todosHorarios,
                                                        double horasRequeridas, String nombre,
                                                        int maxHorasDiarias, Map<Dias, Double> consumoFijo,
                                                        boolean shuffle) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases concentradas en pocos días con sesiones más largas");
        
        // Ordenar días por mayor disponibilidad
        List<Map.Entry<Dias, List<BloqueHorario>>> diasOrdenados = diasViables.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(
                calcularHorasDisponiblesDia(e2.getValue()),
                calcularHorasDisponiblesDia(e1.getValue())
            ))
            .collect(Collectors.toList());
        
        // Si se pide alternativa, alterar el orden (ej. swap de los dos primeros)
        if (shuffle && diasOrdenados.size() > 1) {
             java.util.Collections.swap(diasOrdenados, 0, 1);
        }

        double horasAsignadas = 0;
        
        for (Map.Entry<Dias, List<BloqueHorario>> entry : diasOrdenados) {
            if (horasAsignadas >= horasRequeridas) break;
            
            Dias dia = entry.getKey();
            double consumido = consumoFijo.getOrDefault(dia, 0.0);
            double disponibleDia = maxHorasDiarias - consumido;
            if (disponibleDia <= 0) continue;

            List<BloqueHorario> bloques = entry.getValue();
            // Intentar tomar el máximo posible
            double aTomar = Math.min(disponibleDia, horasRequeridas - horasAsignadas);
            
            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, aTomar);
            
            if (mejorBloque != null) {
                double usarReal = Math.min(mejorBloque.getDuracionHoras(), aTomar);
                HorarioAsignado horario = crearHorario(mejorBloque, usarReal);
                propuesta.agregarHorario(horario);
                horasAsignadas += usarReal;
            }
        }

        if (horasAsignadas < horasRequeridas) {
            double faltante = horasRequeridas - horasAsignadas;
            boolean completado = tryFillWithSmallerBlocks(propuesta, todosHorarios, faltante, null, maxHorasDiarias, consumoFijo);
            if (!completado) {
                propuesta.setDescripcion(propuesta.getDescripcion() + " (parcial)");
            }
        }
        if (propuesta.getTotalHorasSemana() < MIN_HORAS_CLASE) return null;
        return propuesta;
    }
    
    /**
     * Estrategia 3: Distribuida (más días, menos horas por día)
     */
    private PropuestaHorario generarPropuestaDistribuida(Map<Dias, List<BloqueHorario>> diasViables, 
                                                        Map<Dias, List<BloqueHorario>> todosHorarios,
                                                        double horasRequeridas, String nombre,
                                                        int maxHorasDiarias, Map<Dias, Double> consumoFijo,
                                                        boolean shuffle) {
        PropuestaHorario propuesta = new PropuestaHorario(nombre);
        propuesta.setDescripcion("Clases distribuidas en varios días con sesiones cortas");
        
        double horasAsignadas = 0;
        // En distribuida, apuntamos a sesiones cortas (MIN_HORAS_CLASE o un poco más)
        double targetDiario = MIN_HORAS_CLASE; 
        
        List<Dias> diasDisponibles = new ArrayList<>(diasViables.keySet());
        if (shuffle) java.util.Collections.shuffle(diasDisponibles); // Aleatorizar orden

        for (Dias dia : diasDisponibles) {
            if (horasAsignadas >= horasRequeridas) break;

            double consumido = consumoFijo.getOrDefault(dia, 0.0);
            double disponibleDia = maxHorasDiarias - consumido;
            if (disponibleDia < MIN_HORAS_CLASE) continue;
            
            List<BloqueHorario> bloques = diasViables.get(dia);
            double falta = horasRequeridas - horasAsignadas;
            double aPedir = Math.min(targetDiario, Math.min(disponibleDia, falta));

            BloqueHorario mejorBloque = seleccionarMejorBloque(bloques, aPedir);
            
            if (mejorBloque != null) {
                double usarReal = Math.min(mejorBloque.getDuracionHoras(), aPedir);
                HorarioAsignado horario = crearHorario(mejorBloque, usarReal);
                propuesta.agregarHorario(horario);
                horasAsignadas += usarReal;
            }
        }

        if (horasAsignadas < horasRequeridas) {
            double faltante = horasRequeridas - horasAsignadas;
            boolean completado = tryFillWithSmallerBlocks(propuesta, todosHorarios, faltante, diasDisponibles, maxHorasDiarias, consumoFijo);
            if (!completado) {
                propuesta.setDescripcion(propuesta.getDescripcion() + " (parcial)");
            }
        }
        if (propuesta.getTotalHorasSemana() < MIN_HORAS_CLASE) return null;
        return propuesta;
    }

    // ============= GESTIÓN DE RELLENO Y PINNED =============

    private Map<Dias, List<BloqueHorario>> restarHorariosFijados(Map<Dias, List<BloqueHorario>> libres, List<HorarioAsignado> fijos) {
        if (fijos == null || fijos.isEmpty()) return libres;

        Map<Dias, List<BloqueHorario>> resultado = new HashMap<>();
        
        for (Map.Entry<Dias, List<BloqueHorario>> entry : libres.entrySet()) {
            Dias dia = entry.getKey();
            List<HorarioAsignado> fijosDia = fijos.stream().filter(f -> f.getDia() == dia).collect(Collectors.toList());
            
            if (fijosDia.isEmpty()) {
                resultado.put(dia, new ArrayList<>(entry.getValue()));
                continue;
            }

            // Si hay fijos ese día, debemos fragmentar los bloques libres
            List<BloqueHorario> bloquesDia = new ArrayList<>();
            for (BloqueHorario bloque : entry.getValue()) {
                bloquesDia.addAll(fragmentarBloque(bloque, fijosDia));
            }
            if (!bloquesDia.isEmpty()) {
                resultado.put(dia, bloquesDia);
            }
        }
        return resultado;
    }

    private List<BloqueHorario> fragmentarBloque(BloqueHorario bloque, List<HorarioAsignado> fijos) {
        // Algoritmo simple: considerar el bloque y restar cada fijo que se solape
        List<BloqueHorario> result = new ArrayList<>();
        result.add(bloque);

        for (HorarioAsignado fijo : fijos) {
            List<BloqueHorario> nextResult = new ArrayList<>();
            for (BloqueHorario b : result) {
                // Usamos LocalTime para evitar problemas con la fecha base de java.sql.Time
                LocalTime bInicio = b.getHoraInicio().toLocalTime();
                LocalTime bFin = b.getHoraFin().toLocalTime();
                LocalTime fInicio = fijo.getHoraInicio().toLocalTime();
                LocalTime fFin = fijo.getHoraFin().toLocalTime();

                // Si no se tocan, se mantiene (fin <= inicio OR inicio >= fin)
                if (!fInicio.isBefore(bFin) || !fFin.isAfter(bInicio)) {
                    nextResult.add(b);
                } else {
                    // Hay solapamiento, partimos el bloque
                    
                    // Parte antes del fijo (si b.start < f.start)
                    if (bInicio.isBefore(fInicio)) {
                        nextResult.add(new BloqueHorario(b.getDia(), b.getHoraInicio(), fijo.getHoraInicio()));
                    }
                    // Parte despues del fijo (si b.end > f.end)
                    if (bFin.isAfter(fFin)) {
                        nextResult.add(new BloqueHorario(b.getDia(), fijo.getHoraFin(), b.getHoraFin()));
                    }
                }
            }
            result = nextResult;
        }
        return result;
    }
    
    // Método para rellenar huecos
    private boolean tryFillWithSmallerBlocks(PropuestaHorario propuesta, 
                                            Map<Dias, List<BloqueHorario>> todosHorarios, 
                                            double horasFaltantes, 
                                            List<Dias> prioridadDias,
                                            int maxHorasDiarias,
                                            Map<Dias, Double> consumoFijo) {
        if (horasFaltantes <= 0.01) return true;

        // Calcular consumo actual en la propuesta
        Map<Dias, Double> consumoPropuesta = propuesta.getHorarios().stream()
            .collect(Collectors.groupingBy(HorarioAsignado::getDia, Collectors.summingDouble(HorarioAsignado::getDuracionHoras)));

        // Fusionar consumo fijo con propuesta
        consumoFijo.forEach((d, v) -> consumoPropuesta.merge(d, v, Double::sum));

        // Obtener bloques mutables ordenados
        Map<Dias, List<MutableBloque>> bloques = new HashMap<>();
        for (Map.Entry<Dias, List<BloqueHorario>> e : todosHorarios.entrySet()) {
            List<MutableBloque> list = e.getValue().stream()
                .map(b -> new MutableBloque(b.getDia(), b.getHoraInicio(), b.getHoraFin()))
                .sorted((b1,b2) -> b1.inicio.compareTo(b2.inicio))
                .collect(Collectors.toList());
            bloques.put(e.getKey(), list);
        }

        // Orden de iteración
        List<Dias> ordenDias = new ArrayList<>();
        if (prioridadDias != null) {
            for (Dias d: prioridadDias) if (bloques.containsKey(d)) ordenDias.add(d);
        }
        for (Dias d : bloques.keySet()) if (!ordenDias.contains(d)) ordenDias.add(d);

        double faltante = horasFaltantes;
        final double EPS = 0.01;

        for (Dias dia : ordenDias) {
            if (faltante <= EPS) break;
            
            double yaConsumido = consumoPropuesta.getOrDefault(dia, 0.0);
            double capacidadDia = maxHorasDiarias - yaConsumido;
            if (capacidadDia <= EPS) continue;

            List<MutableBloque> bList = bloques.getOrDefault(dia, new ArrayList<>());
            
            // Debemos filtrar de los bloques mutables aquellas partes que YA están usadas en la propuesta actual
            // Esto es complicado porque 'todosHorarios' es la disponibilidad base.
            // Para simplificar: asumimos que las estrategias anteriores (equilibrada, etc) ya han consumido
            // bloques de 'diasViables' que son subconjunto de 'todosHorarios'.
            // Sin embargo, aqui estamos operando sobre 'todosHorarios', que incluye bloques < MIN_HORAS_CLASE.
            // LO CORRECTO: Deberíamos restar de 'bList' los horarios ya asignados en esta propuesta y los fijos.
            // Implementación simplificada: Intentar asignar, verificar colisión con propuesta actual.
            
            for (MutableBloque mb : bList) {
                if (faltante <= EPS) break;
                if (capacidadDia <= EPS) break;

                // Verificar que este bloque mutable no choca con lo que ya pusimos en la propuesta
                // (Los fijos ya deberían estar restados si pasamos 'disponibilidadNeta' como 'todosHorarios', 
                // pero ojo, en la llamada pasamos 'disponibilidadNeta' NO 'todosHorarios' raw).
                // CORRECCION: pasaremos 'disponibilidadNeta' en lugar de 'horariosLibresTotal' a este método.
                
                // Aun asi, choca con lo añadido por la estrategia principal (paso previo).
                if (solapaConPropuesta(mb, propuesta)) continue;

                double dur = mb.duracionHoras();
                if (dur <= 0.01) continue;

                double aUsar = Math.min(Math.min(dur, capacidadDia), faltante);
                if (aUsar <= 0.01) continue;

                Time inicio = mb.inicio;
                LocalTime finLt = inicio.toLocalTime().plusMinutes((long)(aUsar * 60));
                Time fin = Time.valueOf(finLt);
                
                HorarioAsignado nuevo = new HorarioAsignado(dia, inicio, fin);
                propuesta.agregarHorario(nuevo);
                
                // Actualizar contadores
                mb.inicio = fin; // consumimos el inicio del bloque
                capacidadDia -= aUsar;
                faltante -= aUsar;
            }
        }

        return faltante <= EPS;
    }
    
    private boolean solapaConPropuesta(MutableBloque mb, PropuestaHorario p) {
        LocalTime mbInicio = mb.inicio.toLocalTime();
        LocalTime mbFin = mb.fin.toLocalTime();

        for (HorarioAsignado h : p.getHorarios()) {
            if (h.getDia() != mb.dia) continue;
            
            LocalTime hInicio = h.getHoraInicio().toLocalTime();
            LocalTime hFin = h.getHoraFin().toLocalTime();
            
            // Verificar solape (h.start < mb.end AND h.end > mb.start)
            if (hInicio.isBefore(mbFin) && hFin.isAfter(mbInicio)) {
                return true;
            }
        }
        return false;
    }

    // Bloque mutable para consumo parcial
    private static class MutableBloque {
        Dias dia;
        Time inicio;
        Time fin;
        MutableBloque(Dias d, Time i, Time f) { this.dia = d; this.inicio = i; this.fin = f; }
        double duracionHoras() { return (fin.getTime() - inicio.getTime()) / (1000.0 * 60.0 * 60.0); }
    }
    
    // ============= MÉTODOS AUXILIARES =============
    
    private Map<Dias, List<BloqueHorario>> filtrarDiasViables(Map<Dias, List<BloqueHorario>> horariosLibres) {
        Map<Dias, List<BloqueHorario>> diasViables = new HashMap<>();
        
        for (Map.Entry<Dias, List<BloqueHorario>> entry : horariosLibres.entrySet()) {
            List<BloqueHorario> bloquesViables = entry.getValue().stream()
                .filter(bloque -> bloque.getDuracionHoras() >= MIN_HORAS_CLASE)
                .collect(Collectors.toList());
            
            if (!bloquesViables.isEmpty()) {
                diasViables.put(entry.getKey(), bloquesViables);
            }
        }
        
        return diasViables;
    }
    
    private BloqueHorario seleccionarMejorBloque(List<BloqueHorario> bloques, double horasNecesarias) {
        // Preferir bloques que se ajusten mejor a las horas necesarias
        return bloques.stream()
            .filter(b -> b.getDuracionHoras() >= horasNecesarias)
            .min(Comparator.comparingDouble(b -> Math.abs(b.getDuracionHoras() - horasNecesarias)))
            .orElse(bloques.stream()
                .max(Comparator.comparingDouble(BloqueHorario::getDuracionHoras))
                .orElse(null));
    }
    
    private HorarioAsignado crearHorario(BloqueHorario bloque, double horasAUsar) {
        Time horaInicio = bloque.getHoraInicio();
        
        // Calcular hora fin basada en las horas a usar
        LocalTime inicio = horaInicio.toLocalTime();
        LocalTime fin = inicio.plusMinutes((long)(horasAUsar * 60));
        Time horaFin = Time.valueOf(fin);
        
        return new HorarioAsignado(bloque.getDia(), horaInicio, horaFin);
    }

    private HorarioAsignado crearHorario(BloqueHorario bloque, double horasAUsar, Docente docente) {
        HorarioAsignado h = crearHorario(bloque, horasAUsar);
        h.setDocenteInfo(docente);
        return h;
    }
    
    private double calcularHorasDisponiblesDia(List<BloqueHorario> bloques) {
        return bloques.stream()
            .mapToDouble(BloqueHorario::getDuracionHoras)
            .sum();
    }
    
    // ============= CLASES INTERNAS =============
    
    /**
     * Representa una propuesta completa de distribución horaria
     */
    public static class PropuestaHorario {
        private String nombre;
        private String descripcion;
        private List<HorarioAsignado> horarios;
        private double totalHorasSemana;
        private int cantidadDias;
        private double promedioHorasPorDia;
        private double cargaAdicionalDocente;
        private double porcentajeCargaTotal;
        private double score; // Puntuación de calidad de la propuesta
        
        public PropuestaHorario(String nombre) {
            this.nombre = nombre;
            this.horarios = new ArrayList<>();
        }
        
        public void agregarHorario(HorarioAsignado horario) {
            this.horarios.add(horario);
            this.totalHorasSemana += horario.getDuracionHoras();
            
            // Recalcular estadísticas
            this.cantidadDias = (int) horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .count();
            
            this.promedioHorasPorDia = cantidadDias > 0 ? totalHorasSemana / cantidadDias : 0;
        }

        public void ordenarHorarios() {
            this.horarios.sort(Comparator
                .comparing((HorarioAsignado h) -> h.getDia().ordinal())
                .thenComparing(h -> h.getHoraInicio())
                .thenComparing(h -> h.getHoraFin())
                .thenComparing(h -> h.getDocenteId() != null ? h.getDocenteId() : "")
            );
        }
        
        public void calcularMetricas(double cargaActualDocente) {
            this.cargaAdicionalDocente = this.totalHorasSemana;
            this.porcentajeCargaTotal = cargaActualDocente + this.totalHorasSemana;
            
            // Calcular score basado en varios factores
            double scoreDistribucion = calcularScoreDistribucion();
            double scoreCarga = calcularScoreCarga();
            double scoreVariedad = calcularScoreVariedad();
            
            this.score = (scoreDistribucion * 0.4) + (scoreCarga * 0.4) + (scoreVariedad * 0.2);
        }
        
        private double calcularScoreDistribucion() {
            // Preferir 2-3 horas por día (óptimo)
            if (promedioHorasPorDia >= 2 && promedioHorasPorDia <= 3) {
                return 100.0;
            } else if (promedioHorasPorDia < 2) {
                return 70.0;
            } else {
                return 85.0;
            }
        }
        
        private double calcularScoreCarga() {
            // Preferir no sobrecargar (menos del 80% de carga)
            if (porcentajeCargaTotal < 30) {
                return 100.0;
            } else if (porcentajeCargaTotal < 40) {
                return 80.0;
            } else {
                return 60.0;
            }
        }
        
        private double calcularScoreVariedad() {
            // Preferir 2-3 días por semana
            if (cantidadDias >= 2 && cantidadDias <= 3) {
                return 100.0;
            } else if (cantidadDias == 1) {
                return 60.0;
            } else {
                return 80.0;
            }
        }
        
        public boolean esEquivalenteA(PropuestaHorario otra) {
            if (otra == null) return false;
            if (this.cantidadDias != otra.cantidadDias) return false;
            if (Math.abs(this.totalHorasSemana - otra.totalHorasSemana) > 0.5) return false;
            
            // Comparar días utilizados
            List<Dias> diasEsta = this.horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            List<Dias> diasOtra = otra.horarios.stream()
                .map(HorarioAsignado::getDia)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            return diasEsta.equals(diasOtra);
        }
        
        public List<Map<String, Object>> toJSON() {
            return horarios.stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("dia", h.getDia().name());
                map.put("horaInicio", h.getHoraInicio().toString().substring(0, 5));
                map.put("horaFin", h.getHoraFin().toString().substring(0, 5));
                map.put("duracionHoras", h.getDuracionHoras());
                if (h.getDocenteId() != null) {
                    map.put("docenteId", h.getDocenteId());
                }
                if (h.getDocenteNombre() != null && !h.getDocenteNombre().isBlank()) {
                    map.put("docenteNombre", h.getDocenteNombre().trim());
                }
                if (h.getDocentesIds() != null && !h.getDocentesIds().isBlank()) {
                    map.put("docentesIds", Arrays.asList(h.getDocentesIds().split(",")));
                }
                if (h.getDocentesNombres() != null && !h.getDocentesNombres().isBlank()) {
                    map.put("docentesNombres", h.getDocentesNombres().trim());
                }
                return map;
            }).collect(Collectors.toList());
        }
        
        // Getters
        public String getNombre() { return nombre; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public List<HorarioAsignado> getHorarios() { return horarios; }
        public double getTotalHorasSemana() { return totalHorasSemana; }
        public int getCantidadDias() { return cantidadDias; }
        public double getPromedioHorasPorDia() { return promedioHorasPorDia; }
        public double getCargaAdicionalDocente() { return cargaAdicionalDocente; }
        public double getPorcentajeCargaTotal() { return porcentajeCargaTotal; }
        public double getScore() { return score; }
    }
    
    /**
     * Representa un horario específico asignado
     */
    public static class HorarioAsignado {
        private Dias dia;
        private Time horaInicio;
        private Time horaFin;
        private String docenteId;
        private String docenteNombre;
        private String docentesIds;
        private String docentesNombres;
        
        public HorarioAsignado(Dias dia, Time horaInicio, Time horaFin) {
            this.dia = dia;
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }
        
        public double getDuracionHoras() {
            long diffMs = horaFin.getTime() - horaInicio.getTime();
            return diffMs / (1000.0 * 60.0 * 60.0);
        }
        
        public Dias getDia() { return dia; }
        public Time getHoraInicio() { return horaInicio; }
        public Time getHoraFin() { return horaFin; }
        public String getDocenteId() { return docenteId; }
        public String getDocenteNombre() { return docenteNombre; }
        public void setDocenteId(String docenteId) { this.docenteId = docenteId; }
        public String getDocentesIds() { return docentesIds; }
        public String getDocentesNombres() { return docentesNombres; }
        public void setDocentesIds(String docentesIds) { this.docentesIds = docentesIds; }

        public void setDocenteInfo(Docente docente) {
            if (docente == null) return;
            this.docenteId = docente.getId() != null ? docente.getId().toString() : null;
            this.docenteNombre = (docente.getNombre() != null ? docente.getNombre() : "") +
                                 (docente.getApellido() != null ? " " + docente.getApellido() : "");
        }

        public void setDocentesInfo(List<Docente> docentes) {
            if (docentes == null || docentes.isEmpty()) return;
            this.docentesIds = docentes.stream()
                .filter(d -> d.getId() != null)
                .map(d -> d.getId().toString())
                .collect(Collectors.joining(","));
            this.docentesNombres = docentes.stream()
                .map(d -> (d.getNombre() != null ? d.getNombre() : "") + (d.getApellido() != null ? " " + d.getApellido() : ""))
                .map(String::trim)
                .collect(Collectors.joining(", "));
        }
        
        public Horario toHorario() {
            Horario horario = new Horario();
            horario.setDia(this.dia);
            horario.setHoraInicio(this.horaInicio);
            horario.setHoraFin(this.horaFin);
            return horario;
        }
    }
}
