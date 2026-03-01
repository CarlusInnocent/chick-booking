package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.service.BookingService;
import com.example.chicke_booking.service.ChickService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final ChickService chickService;

    @GetMapping
    public String listBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            Model model
    ) {
        List<Booking> bookings;
        
        if (status != null) {
            bookings = bookingService.getBookingsByStatus(status);
        } else if (search != null && !search.isBlank()) {
            // Search by customer name or receipt number
            bookings = bookingService.searchByCustomerName(search);
            // Also search by receipt number and merge results
            List<Booking> receiptResults = bookingService.searchByReceiptNumber(search);
            for (Booking b : receiptResults) {
                if (!bookings.contains(b)) {
                    bookings.add(b);
                }
            }
        } else {
            bookings = bookingService.getAllBookings();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);
        return "admin/bookings/list";
    }

    @GetMapping("/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        model.addAttribute("booking", booking);
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/bookings/details";
    }

    @GetMapping("/new")
    public String newBookingForm(Model model) {
        List<Chick> chicks = chickService.getActiveChicks();
        model.addAttribute("chicks", chicks);
        model.addAttribute("minDate", LocalDate.now());
        return "admin/bookings/form";
    }

    @PostMapping("/create")
    public String createBooking(
            @RequestParam String customerName,
            @RequestParam String location,
            @RequestParam String phone,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam(required = false) String notes,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Map<Long, Integer> chickQuantities = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (entry.getKey().startsWith("quantity_")) {
                    Long chickId = Long.parseLong(entry.getKey().substring(9));
                    Integer quantity = entry.getValue().isEmpty() ? 0 : Integer.parseInt(entry.getValue());
                    if (quantity > 0) {
                        chickQuantities.put(chickId, quantity);
                    }
                }
            }

            if (chickQuantities.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select at least one chick.");
                return "redirect:/admin/bookings/new";
            }

            // Calculate total quantity and validate minimum order of 100 chicks
            int totalQuantity = chickQuantities.values().stream().mapToInt(Integer::intValue).sum();
            if (totalQuantity < 100) {
                redirectAttributes.addFlashAttribute("error", "Minimum order is 100 chicks. You selected " + totalQuantity + " chicks.");
                return "redirect:/admin/bookings/new";
            }

            Booking booking = bookingService.createBooking(
                    customerName, location, phone, pickupDate, notes, chickQuantities
            );

            redirectAttributes.addFlashAttribute("success", "Booking created successfully!");
            return "redirect:/admin/bookings/" + booking.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/bookings/new";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public String editBookingForm(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        model.addAttribute("booking", booking);
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/bookings/edit";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public String updateBooking(
            @PathVariable Long id,
            @RequestParam String customerName,
            @RequestParam String location,
            @RequestParam String phone,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam(required = false) String notes,
            @RequestParam BookingStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bookingService.updateBooking(id, customerName, location, phone, pickupDate, notes, status);
            redirectAttributes.addFlashAttribute("success", "Booking updated successfully!");
            return "redirect:/admin/bookings/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/bookings/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            bookingService.updateBookingStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public String deleteBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.deleteBooking(id);
            redirectAttributes.addFlashAttribute("success", "Booking deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}
