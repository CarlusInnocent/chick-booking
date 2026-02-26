package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/locations")
@PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
@RequiredArgsConstructor
public class AdminLocationController {

    private final LocationService locationService;

    @GetMapping
    public String listLocations(Model model) {
        List<Location> locations = locationService.getAllLocations();
        model.addAttribute("locations", locations);
        return "admin/locations/list";
    }

    @GetMapping("/new")
    public String newLocationForm() {
        return "admin/locations/form";
    }

    @PostMapping("/create")
    public String createLocation(
            @RequestParam String name,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String description,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) BigDecimal deliveryFee,
            RedirectAttributes redirectAttributes
    ) {
        try {
            locationService.createLocation(name, district, region, description, latitude, longitude, deliveryFee);
            redirectAttributes.addFlashAttribute("success", "Location added successfully!");
            return "redirect:/admin/locations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/locations/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editLocationForm(@PathVariable Long id, Model model) {
        Location location = locationService.getLocationById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found"));
        model.addAttribute("location", location);
        return "admin/locations/edit";
    }

    @PostMapping("/{id}/update")
    public String updateLocation(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String description,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) BigDecimal deliveryFee,
            @RequestParam(required = false) boolean active,
            RedirectAttributes redirectAttributes
    ) {
        try {
            locationService.updateLocation(id, name, district, region, description, latitude, longitude, deliveryFee, active);
            redirectAttributes.addFlashAttribute("success", "Location updated successfully!");
            return "redirect:/admin/locations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/locations/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            locationService.deleteLocation(id);
            redirectAttributes.addFlashAttribute("success", "Location deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/locations";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            locationService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "Status toggled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/locations";
    }
}
