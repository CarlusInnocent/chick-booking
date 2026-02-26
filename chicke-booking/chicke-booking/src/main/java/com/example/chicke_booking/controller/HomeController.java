package com.example.chicke_booking.controller;

import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.service.ChickService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ChickService chickService;

    @GetMapping("/")
    public String home(Model model) {
        List<Chick> featuredChicks = chickService.getActiveChicks().stream()
                .limit(6)
                .toList();
        model.addAttribute("featuredChicks", featuredChicks);
        return "customer/index";
    }

    @GetMapping("/catalog")
    public String catalog(Model model) {
        List<Chick> chicks = chickService.getActiveChicks();
        model.addAttribute("chicks", chicks);
        return "customer/catalog";
    }
}
