package com.example.chicke_booking.controller;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.BookingSetting;
import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.service.BookingService;
import com.example.chicke_booking.service.BookingSettingService;
import com.example.chicke_booking.service.ChickService;
import com.example.chicke_booking.service.PdfService;
import com.example.chicke_booking.service.PesaPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
@Slf4j
public class CustomerBookingController {

    private final BookingService bookingService;
    private final ChickService chickService;
    private final BookingSettingService bookingSettingService;
    private final PdfService pdfService;
    private final PesaPalService pesaPalService;

    @GetMapping("/new")
    public String showBookingForm(Model model) {
        List<Chick> chicks = chickService.getActiveChicks();
        model.addAttribute("chicks", chicks);
        
        // Get the next available booking date set by admin
        BookingSetting bookingSetting = bookingSettingService.getBookingSetting().orElse(null);
        LocalDate nextBookingDate = bookingSetting != null ? bookingSetting.getNextBookingDate() : null;
        boolean bookingEnabled = bookingSetting == null || bookingSetting.isBookingEnabled();
        String bookingMessage = bookingSetting != null ? bookingSetting.getMessage() : null;
        
        model.addAttribute("nextBookingDate", nextBookingDate);
        model.addAttribute("bookingEnabled", bookingEnabled);
        model.addAttribute("bookingMessage", bookingMessage);
        model.addAttribute("minDate", nextBookingDate != null ? nextBookingDate : LocalDate.now().plusDays(1));
        
        return "customer/booking-form";
    }

    @PostMapping("/submit")
    public String submitBooking(
            @RequestParam String customerName,
            @RequestParam String location,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Extract chick quantities from form params (format: quantity_1, quantity_2, etc.)
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
                return "redirect:/booking/new";
            }

            // Calculate total quantity and validate minimum order of 100 chicks
            int totalQuantity = chickQuantities.values().stream().mapToInt(Integer::intValue).sum();
            if (totalQuantity < 100) {
                redirectAttributes.addFlashAttribute("error", "Minimum order is 100 chicks. You selected " + totalQuantity + " chicks.");
                return "redirect:/booking/new";
            }

            Booking booking = bookingService.createBooking(
                    customerName, location, phone, email, pickupDate, notes, chickQuantities, latitude, longitude
            );

            // Redirect to PesaPal for payment
            try {
                String pesaPalRedirectUrl = pesaPalService.submitOrder(booking);
                return "redirect:" + pesaPalRedirectUrl;
            } catch (Exception paymentEx) {
                log.warn("PesaPal payment initiation failed for booking {}, showing confirmation instead",
                        booking.getReceiptNumber(), paymentEx);
                redirectAttributes.addFlashAttribute("success", true);
                redirectAttributes.addFlashAttribute("paymentWarning",
                        "Booking created but online payment could not be initiated. Please contact us for payment.");
                return "redirect:/booking/confirmation/" + booking.getId();
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
            return "redirect:/booking/new";
        }
    }

    @GetMapping("/confirmation/{id}")
    public String showConfirmation(@PathVariable Long id,
                                   @RequestParam(value = "paid", required = false) Boolean paid,
                                   @RequestParam(value = "status", required = false) String paymentStatusParam,
                                   Model model) {
        Booking booking = bookingService.getBookingByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        model.addAttribute("booking", booking);
        model.addAttribute("paid", paid);
        model.addAttribute("paymentStatusParam", paymentStatusParam);
        return "customer/booking-confirmation";
    }

    @GetMapping("/pay/{id}")
    public String retryPayment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.getBookingByIdWithItems(id)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

            String pesaPalRedirectUrl = pesaPalService.submitOrder(booking);
            return "redirect:" + pesaPalRedirectUrl;
        } catch (Exception e) {
            log.error("Retry payment failed for booking {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Could not initiate payment. Please try again later.");
            return "redirect:/booking/confirmation/" + id;
        }
    }

    @GetMapping("/receipt/{id}/download")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        try {
            Booking booking = bookingService.getBookingByIdWithItems(id)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            
            byte[] pdfBytes = pdfService.generateReceipt(booking);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "receipt-" + booking.getReceiptNumber() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public String showSearchForm() {
        return "customer/receipt-search";
    }

    @GetMapping("/search/receipt")
    public String searchReceipt(@RequestParam(required = false) String receiptNumber, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please enter a receipt number");
                return "redirect:/booking/search";
            }

            String cleanReceiptNumber = receiptNumber.trim();
            Booking booking = bookingService.getBookingByReceiptNumberWithItems(cleanReceiptNumber)
                    .orElse(null);
            
            if (booking == null) {
                redirectAttributes.addFlashAttribute("error", "No booking found with receipt number: " + cleanReceiptNumber);
                return "redirect:/booking/search";
            }
            
            model.addAttribute("booking", booking);
            return "customer/booking-confirmation";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error searching for booking. Please try again.");
            return "redirect:/booking/search";
        }
    }
}
