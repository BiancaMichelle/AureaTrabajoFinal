package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicoController {
    @GetMapping("/")
    public String publico(Model model) {

        return "publico";
    }

}