package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.model.Alumno;
import com.example.demo.model.Charla;
import com.example.demo.model.Ciudad;
import com.example.demo.model.Curso;
import com.example.demo.model.Formacion;
import com.example.demo.model.InstitucionAlumno;
import com.example.demo.model.OfertaAcademica;
import com.example.demo.model.Pais;
import com.example.demo.model.Provincia;
import com.example.demo.model.Seminario;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.OfertaAcademicaRepository;
import com.example.demo.service.InstitucionService;
import com.example.demo.service.LocacionAPIService;
import com.example.demo.service.RegistroService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class PublicoController {


}