package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.model.entity.User;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final BookingService bookingService;
    private final ChickService chickService;
    private final LocationService locationService;
    private final UserService userService;
    private final PdfService pdfService;

    /**
     * Export all bookings to PDF
     */
    @GetMapping("/bookings")
    public ResponseEntity<byte[]> exportAllBookings() {
        try {
            List<Booking> bookings = bookingService.getAllBookings();
            byte[] pdfBytes = pdfService.generateBookingsReport(bookings, "ALL BOOKINGS REPORT");
            return createPdfResponse(pdfBytes, "all-bookings-report");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export bookings by status to PDF
     */
    @GetMapping("/bookings/status/{status}")
    public ResponseEntity<byte[]> exportBookingsByStatus(@PathVariable BookingStatus status) {
        try {
            List<Booking> bookings = bookingService.getBookingsByStatus(status);
            byte[] pdfBytes = pdfService.generateBookingsReport(bookings, status.name() + " BOOKINGS REPORT");
            return createPdfResponse(pdfBytes, status.name().toLowerCase() + "-bookings-report");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export pending bookings to PDF
     */
    @GetMapping("/bookings/pending")
    public ResponseEntity<byte[]> exportPendingBookings() {
        return exportBookingsByStatus(BookingStatus.PENDING);
    }

    /**
     * Export confirmed bookings to PDF
     */
    @GetMapping("/bookings/confirmed")
    public ResponseEntity<byte[]> exportConfirmedBookings() {
        return exportBookingsByStatus(BookingStatus.CONFIRMED);
    }

    /**
     * Export completed bookings to PDF
     */
    @GetMapping("/bookings/completed")
    public ResponseEntity<byte[]> exportCompletedBookings() {
        return exportBookingsByStatus(BookingStatus.COMPLETED);
    }

    /**
     * Export single booking receipt to PDF (Admin version)
     */
    @GetMapping("/booking/{id}/receipt")
    public ResponseEntity<byte[]> exportBookingReceipt(@PathVariable Long id) {
        try {
            Booking booking = bookingService.getBookingByIdWithItems(id)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            byte[] pdfBytes = pdfService.generateReceipt(booking);
            return createPdfResponse(pdfBytes, "receipt-" + booking.getReceiptNumber());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export chicks inventory to PDF (Admin only)
     */
    @GetMapping("/chicks")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<byte[]> exportChicks() {
        try {
            List<Chick> chicks = chickService.getAllChicks();
            byte[] pdfBytes = pdfService.generateChicksReport(chicks);
            return createPdfResponse(pdfBytes, "chicks-inventory-report");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export locations to PDF (Admin only)
     */
    @GetMapping("/locations")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<byte[]> exportLocations() {
        try {
            List<Location> locations = locationService.getAllLocations();
            byte[] pdfBytes = pdfService.generateLocationsReport(locations);
            return createPdfResponse(pdfBytes, "locations-report");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export operators to PDF (Admin only)
     */
    @GetMapping("/operators")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN')")
    public ResponseEntity<byte[]> exportOperators() {
        try {
            List<User> operators = userService.getOperators();
            byte[] pdfBytes = pdfService.generateOperatorsReport(operators);
            return createPdfResponse(pdfBytes, "operators-report");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfBytes, String filename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename + "-" + timestamp + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
