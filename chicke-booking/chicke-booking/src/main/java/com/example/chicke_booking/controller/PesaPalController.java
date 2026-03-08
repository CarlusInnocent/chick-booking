package com.example.chicke_booking.controller;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.enums.PaymentStatus;
import com.example.chicke_booking.repository.BookingRepository;
import com.example.chicke_booking.service.PesaPalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PesaPalController {

    private final PesaPalService pesaPalService;
    private final BookingRepository bookingRepository;

    /**
     * PesaPal redirects the customer here after payment attempt.
     * Query params: OrderTrackingId, OrderMerchantReference, OrderNotificationType
     */
    @GetMapping("/callback")
    public String paymentCallback(
            @RequestParam("OrderTrackingId") String orderTrackingId,
            @RequestParam("OrderMerchantReference") String merchantReference,
            @RequestParam(value = "OrderNotificationType", required = false) String notificationType,
            Model model) {

        log.info("PesaPal callback: trackingId={}, merchantRef={}, type={}",
                orderTrackingId, merchantReference, notificationType);

        // Check transaction status with PesaPal
        PaymentStatus status = pesaPalService.getTransactionStatus(orderTrackingId);

        Optional<Booking> bookingOpt = bookingRepository.findByPesapalOrderTrackingId(orderTrackingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            model.addAttribute("booking", booking);
            model.addAttribute("paymentStatus", status);

            if (status == PaymentStatus.COMPLETED) {
                return "redirect:/booking/confirmation/" + booking.getReceiptNumber() + "?paid=true";
            } else {
                return "redirect:/booking/confirmation/" + booking.getReceiptNumber() + "?paid=false&status=" + status;
            }
        }

        // Fallback if booking not found
        log.warn("No booking found for PesaPal tracking ID: {}", orderTrackingId);
        return "redirect:/?error=payment";
    }

    /**
     * PesaPal sends IPN (Instant Payment Notification) here.
     * Query params: OrderTrackingId, OrderMerchantReference, OrderNotificationType
     */
    @GetMapping("/ipn")
    @ResponseBody
    public ResponseEntity<String> ipnNotification(
            @RequestParam("OrderTrackingId") String orderTrackingId,
            @RequestParam("OrderMerchantReference") String merchantReference,
            @RequestParam(value = "OrderNotificationType", required = false) String notificationType) {

        log.info("PesaPal IPN: trackingId={}, merchantRef={}, type={}",
                orderTrackingId, merchantReference, notificationType);

        PaymentStatus status = pesaPalService.getTransactionStatus(orderTrackingId);
        log.info("IPN transaction status resolved to: {}", status);

        return ResponseEntity.ok("OK");
    }
}
