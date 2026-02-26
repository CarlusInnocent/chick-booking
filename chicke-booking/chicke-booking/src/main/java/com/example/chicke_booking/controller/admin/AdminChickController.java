package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.service.ChickService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/chicks")
@PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
@RequiredArgsConstructor
public class AdminChickController {

    private final ChickService chickService;

    @GetMapping
    public String listChicks(Model model) {
        List<Chick> chicks = chickService.getAllChicks();
        model.addAttribute("chicks", chicks);
        return "admin/chicks/list";
    }

    @GetMapping("/new")
    public String newChickForm() {
        return "admin/chicks/form";
    }

    @PostMapping("/create")
    public String createChick(
            @RequestParam String breed,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes
    ) {
        try {
            chickService.createChick(breed, description, price, imageUrl);
            redirectAttributes.addFlashAttribute("success", "Chick breed added successfully!");
            return "redirect:/admin/chicks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/chicks/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editChickForm(@PathVariable Long id, Model model) {
        Chick chick = chickService.getChickById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chick not found"));
        model.addAttribute("chick", chick);
        return "admin/chicks/edit";
    }

    @PostMapping("/{id}/update")
    public String updateChick(
            @PathVariable Long id,
            @RequestParam String breed,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            chickService.updateChick(id, breed, description, price, imageUrl, active);
            redirectAttributes.addFlashAttribute("success", "Chick breed updated successfully!");
            return "redirect:/admin/chicks";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/chicks/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteChick(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            chickService.deleteChick(id);
            redirectAttributes.addFlashAttribute("success", "Chick breed deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/chicks";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            chickService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "Status toggled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/chicks";
    }
}
