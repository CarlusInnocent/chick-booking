package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.User;
import com.example.chicke_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/operators")
@PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
@RequiredArgsConstructor
public class AdminOperatorController {

    private final UserService userService;

    @GetMapping
    public String listOperators(Model model) {
        List<User> operators = userService.getOperators();
        model.addAttribute("operators", operators);
        return "admin/operators/list";
    }

    @GetMapping("/new")
    public String newOperatorForm() {
        return "admin/operators/form";
    }

    @PostMapping("/create")
    public String createOperator(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (userService.existsByUsername(username)) {
                redirectAttributes.addFlashAttribute("error", "Username already exists!");
                return "redirect:/admin/operators/new";
            }

            String createdBy = authentication.getName();
            userService.createOperator(username, password, fullName, createdBy);
            redirectAttributes.addFlashAttribute("success", "Operator created successfully!");
            return "redirect:/admin/operators";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/operators/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editOperatorForm(@PathVariable Long id, Model model) {
        User operator = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found"));
        model.addAttribute("operator", operator);
        return "admin/operators/edit";
    }

    @PostMapping("/{id}/update")
    public String updateOperator(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam(required = false) boolean enabled,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.updateUser(id, fullName, enabled);
            redirectAttributes.addFlashAttribute("success", "Operator updated successfully!");
            return "redirect:/admin/operators";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/operators/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteOperator(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Operator deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/operators";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password reset successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/operators";
    }
}
