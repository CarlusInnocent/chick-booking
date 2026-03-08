package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.BookingSetting;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.model.enums.PaymentStatus;
import com.example.chicke_booking.service.BookingService;
import com.example.chicke_booking.service.BookingSettingService;
import com.example.chicke_booking.service.ChickService;
import com.example.chicke_booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final BookingService bookingService;
    private final ChickService chickService;
    private final UserService userService;
    private final BookingSettingService bookingSettingService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Booking statistics
        model.addAttribute("totalBookings", bookingService.getTotalBookingsCount());
        model.addAttribute("pendingBookings", bookingService.countByStatus(BookingStatus.PENDING));
        model.addAttribute("confirmedBookings", bookingService.countByStatus(BookingStatus.CONFIRMED));
        model.addAttribute("completedBookings", bookingService.countByStatus(BookingStatus.COMPLETED));
        model.addAttribute("totalRevenue", bookingService.getTotalRevenue());

        // Payment statistics
        model.addAttribute("paidBookings", bookingService.countByPaymentStatus(PaymentStatus.COMPLETED));
        model.addAttribute("unpaidBookings", bookingService.countByPaymentStatus(PaymentStatus.PENDING));

        // Recent bookings
        model.addAttribute("recentBookings", bookingService.getRecentBookings(5));

        // Counts
        model.addAttribute("totalChicks", chickService.getAllChicks().size());
        model.addAttribute("totalOperators", userService.getOperators().size());

        // Booking settings
        BookingSetting bookingSetting = bookingSettingService.getBookingSetting().orElse(null);
        model.addAttribute("bookingSetting", bookingSetting);
        model.addAttribute("minDate", LocalDate.now());

        return "admin/dashboard";
    }

    @PostMapping("/settings/next-booking-date")
    public String updateNextBookingDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextBookingDate,
            @RequestParam(defaultValue = "true") boolean bookingEnabled,
            @RequestParam(required = false) String message,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String username = authentication.getName();
            bookingSettingService.updateNextBookingDate(nextBookingDate, bookingEnabled, message, username);
            redirectAttributes.addFlashAttribute("success", "Next booking date updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating booking date: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
