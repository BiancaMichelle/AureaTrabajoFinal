package com.example.demo.controller;

import com.example.demo.model.Alumno;
import com.example.demo.model.Charla;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Formacion;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Seminario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private OfertaAcademicaRepository ofertaAcademicaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        List<OfertaAcademica> todasLasOfertas = ofertaAcademicaRepository.findAll();
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();

        // Estadísticas de Ofertas Académicas
        long totalCursos = todasLasOfertas.stream().filter(o -> o instanceof Curso).count();
        long totalFormaciones = todasLasOfertas.stream().filter(o -> o instanceof Formacion).count();
        long totalSeminarios = todasLasOfertas.stream().filter(o -> o instanceof Seminario).count();
        long totalCharlas = todasLasOfertas.stream().filter(o -> o instanceof Charla).count();
        long ofertasActivas = todasLasOfertas.stream().filter(o -> o.getEstaActiva()).count();

        // Estadísticas de Usuarios
        long totalAlumnos = todosLosUsuarios.stream().filter(u -> u instanceof Alumno).count();
        long totalDocentes = todosLosUsuarios.stream().filter(u -> u instanceof Docente).count();

        // Agregando datos al modelo
        model.addAttribute("totalCursos", totalCursos);
        model.addAttribute("totalFormaciones", totalFormaciones);
        model.addAttribute("totalSeminarios", totalSeminarios);
        model.addAttribute("totalCharlas", totalCharlas);
        model.addAttribute("ofertasActivas", ofertasActivas);
        model.addAttribute("totalOfertas", todasLasOfertas.size());
        model.addAttribute("totalAlumnos", totalAlumnos);
        model.addAttribute("totalDocentes", totalDocentes);

        // Datos para los gráficos (simplificado)
        // En un caso real, podrías tener consultas más complejas
        model.addAttribute("inscripcionesCount", 125); // Placeholder
        model.addAttribute("abandonosCount", 15); // Placeholder

        return "admin/panelAdmin";
    }

    @GetMapping("/admin/gestion-usuarios")
    public String gestionUsuarios(Model model) {
        // Aquí puedes agregar lógica para cargar datos necesarios para la página de gestión de usuarios
        return "admin/gestionUsuarios";
    }

    @GetMapping("/admin/gestion-ofertas")
    public String gestionOfertas(Model model) {
        // Aquí puedes agregar lógica para cargar datos necesarios para la página de gestión de ofertas académicas
        return "admin/gestionOfertas";
    }
}

