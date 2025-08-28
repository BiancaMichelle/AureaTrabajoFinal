package com.example.demo.controller;

import com.example.demo.service.RegistroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @GetMapping("/register")
    public String showForm(@RequestParam(value="error", required=false) String error,
                           Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @RequestParam String username,
            @RequestParam String password,
            RedirectAttributes ra) {
        try {
            registroService.registrarUsuario(
                username,
                password,
                "ROLE_ESTUDIANTE"         
            );
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            ra.addAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }
}
