package com.example.chicke_booking.service;

import com.example.chicke_booking.config.PesaPalConfig;
import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.enums.PaymentStatus;
import com.example.chicke_booking.repository.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PesaPalService {

    private final PesaPalConfig pesaPalConfig;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // Cached auth token
    private String cachedToken;
    private Instant tokenExpiry;

    // Cached IPN id
    private String registeredIpnId;

    /**
     * Get an OAuth bearer token from PesaPal API v3.
     * Caches the token until it expires.
     */
    public String getAuthToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String url = pesaPalConfig.getApiUrl() + "/api/Auth/RequestToken";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("consumer_key", pesaPalConfig.getConsumerKey());
        body.put("consumer_secret", pesaPalConfig.getConsumerSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBody = response.getBody();

            if (responseBody != null && responseBody.has("token")) {
                cachedToken = responseBody.get("token").asText();
                String expiryDate = responseBody.get("expiryDate").asText();
                tokenExpiry = Instant.parse(expiryDate);
                log.info("PesaPal auth token obtained, expires at {}", expiryDate);
                return cachedToken;
            }
            throw new RuntimeException("Failed to get PesaPal auth token: " + responseBody);
        } catch (Exception e) {
            log.error("Error obtaining PesaPal auth token", e);
            throw new RuntimeException("Could not authenticate with PesaPal", e);
        }
    }

    /**
     * Register IPN (Instant Payment Notification) URL with PesaPal.
     * Returns the IPN registration ID needed when submitting orders.
     */
    public String registerIpnUrl() {
        if (registeredIpnId != null) {
            return registeredIpnId;
        }

        String token = getAuthToken();
        String url = pesaPalConfig.getApiUrl() + "/api/URLSetup/RegisterIPN";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("url", pesaPalConfig.getIpnUrl());
        body.put("ipn_notification_type", "GET");

        HttpHeaders headers = buildAuthHeaders(token);
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBody = response.getBody();

            if (responseBody != null && responseBody.has("ipn_id")) {
                registeredIpnId = responseBody.get("ipn_id").asText();
                log.info("PesaPal IPN registered with id: {}", registeredIpnId);
                return registeredIpnId;
            }
            throw new RuntimeException("Failed to register IPN URL: " + responseBody);
        } catch (Exception e) {
            log.error("Error registering PesaPal IPN URL", e);
            throw new RuntimeException("Could not register IPN URL with PesaPal", e);
        }
    }

    /**
     * Submit an order to PesaPal for the given booking.
     * Returns the redirect URL for the customer to complete payment.
     */
    public String submitOrder(Booking booking) {
        String token = getAuthToken();
        String ipnId = registerIpnUrl();
        String url = pesaPalConfig.getApiUrl() + "/api/Transactions/SubmitOrderRequest";

        String merchantReference = booking.getReceiptNumber();

        ObjectNode billingAddress = objectMapper.createObjectNode();
        billingAddress.put("phone_number", booking.getPhone());
        if (booking.getEmail() != null) {
            billingAddress.put("email_address", booking.getEmail());
        }
        billingAddress.put("first_name", booking.getCustomerName());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("id", UUID.randomUUID().toString());
        body.put("currency", "UGX");
        body.put("amount", booking.getTotalAmount().doubleValue());
        body.put("description", "Chick booking #" + booking.getReceiptNumber());
        body.put("callback_url", pesaPalConfig.getCallbackUrl());
        body.put("notification_id", ipnId);
        body.set("billing_address", billingAddress);
        body.put("branch", "Suubi Agrovet");
        body.put("merchant_reference", merchantReference);

        HttpHeaders headers = buildAuthHeaders(token);
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBody = response.getBody();

            if (responseBody != null && responseBody.has("order_tracking_id")) {
                String orderTrackingId = responseBody.get("order_tracking_id").asText();
                String redirectUrl = responseBody.get("redirect_url").asText();

                // Save tracking ID on the booking
                booking.setPesapalOrderTrackingId(orderTrackingId);
                bookingRepository.save(booking);

                log.info("PesaPal order submitted for booking {}. Tracking ID: {}", booking.getReceiptNumber(), orderTrackingId);
                return redirectUrl;
            }

            String errorMsg = responseBody != null ? responseBody.toString() : "null response";
            throw new RuntimeException("PesaPal order submission failed: " + errorMsg);
        } catch (Exception e) {
            log.error("Error submitting PesaPal order for booking {}", booking.getReceiptNumber(), e);
            throw new RuntimeException("Could not submit order to PesaPal", e);
        }
    }

    /**
     * Query PesaPal for the transaction status using the order tracking ID.
     * Updates the booking's payment status accordingly.
     */
    public PaymentStatus getTransactionStatus(String orderTrackingId) {
        String token = getAuthToken();
        String url = pesaPalConfig.getApiUrl() + "/api/Transactions/GetTransactionStatus?orderTrackingId=" + orderTrackingId;

        HttpHeaders headers = buildAuthHeaders(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
            JsonNode responseBody = response.getBody();

            if (responseBody != null) {
                int statusCode = responseBody.has("payment_status_description")
                        ? responseBody.get("status_code").asInt()
                        : -1;

                String paymentMethod = responseBody.has("payment_method") ? responseBody.get("payment_method").asText() : null;
                String confirmationCode = responseBody.has("confirmation_code") ? responseBody.get("confirmation_code").asText() : null;

                log.info("PesaPal transaction status for {}: code={}, method={}, confirmation={}",
                        orderTrackingId, statusCode, paymentMethod, confirmationCode);

                // Update booking with transaction details
                bookingRepository.findByPesapalOrderTrackingId(orderTrackingId).ifPresent(booking -> {
                    if (paymentMethod != null) {
                        booking.setPaymentMethod(paymentMethod);
                    }
                    if (confirmationCode != null) {
                        booking.setPesapalTransactionId(confirmationCode);
                    }

                    PaymentStatus newStatus = mapStatusCode(statusCode);
                    booking.setPaymentStatus(newStatus);
                    bookingRepository.save(booking);

                    // Notify admin of payment result
                    if (newStatus == PaymentStatus.COMPLETED || newStatus == PaymentStatus.FAILED) {
                        emailService.sendPaymentNotification(booking, newStatus);
                    }
                });

                return mapStatusCode(statusCode);
            }
            return PaymentStatus.PENDING;
        } catch (Exception e) {
            log.error("Error checking PesaPal transaction status for {}", orderTrackingId, e);
            return PaymentStatus.PENDING;
        }
    }

    private PaymentStatus mapStatusCode(int statusCode) {
        return switch (statusCode) {
            case 0 -> PaymentStatus.PENDING;    // Invalid
            case 1 -> PaymentStatus.COMPLETED;  // Completed
            case 2 -> PaymentStatus.FAILED;     // Failed
            case 3 -> PaymentStatus.CANCELLED;  // Reversed
            default -> PaymentStatus.PENDING;
        };
    }

    private HttpHeaders buildAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.setBearerAuth(token);
        return headers;
    }
}
