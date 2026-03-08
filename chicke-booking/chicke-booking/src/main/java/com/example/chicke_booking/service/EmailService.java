package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.BookingItem;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.model.enums.PaymentStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.admin-email}")
    private String adminEmail;

    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("en", "UG"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Send booking notifications - to admin and optionally to customer if they provided email
     */
    @Async
    public void sendNewBookingNotification(Booking booking) {
        // Send notification to admin about new booking
        try {
            sendAdminNewBookingAlert(booking);
            log.info("Admin notification sent for booking #{}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send admin notification for booking #{}: {}", booking.getId(), e.getMessage());
        }
        
        // Send receipt to customer if they provided an email (separate try-catch so admin failure doesn't affect customer)
        if (booking.getEmail() != null && !booking.getEmail().isBlank()) {
            try {
                sendCustomerReceipt(booking);
                log.info("Customer receipt sent for booking #{} to {}", booking.getId(), booking.getEmail());
            } catch (Exception e) {
                log.error("Failed to send customer receipt for booking #{} to {}: {}", 
                    booking.getId(), booking.getEmail(), e.getMessage());
            }
        } else {
            log.info("No customer email provided for booking #{}, skipping receipt", booking.getId());
        }
    }

    /**
     * Send receipt/confirmation email to customer
     */
    private void sendCustomerReceipt(Booking booking) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(booking.getEmail());
        helper.setSubject("🐣 Your Booking #" + booking.getId() + " - Suubi Agrovet LTD");
        helper.setText(buildCustomerReceiptEmail(booking), true);

        mailSender.send(message);
    }

    /**
     * Send email to admin when a new booking is received
     */
    private void sendAdminNewBookingAlert(Booking booking) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(adminEmail);
        helper.setSubject("🐣 New Booking #" + booking.getId() + " - " + booking.getCustomerName());
        helper.setText(buildAdminNewBookingEmail(booking), true);

        mailSender.send(message);
        log.info("Admin notification sent for new booking #{}", booking.getId());
    }

    /**
     * Send email when booking status changes
     */
    @Async
    public void sendStatusChangeNotification(Booking booking, BookingStatus oldStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("📋 Booking #" + booking.getId() + " Status: " + booking.getStatus().name());
            helper.setText(buildStatusChangeEmail(booking, oldStatus), true);

            mailSender.send(message);
            log.info("Status change notification sent for booking #{}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send status change notification for booking #{}: {}", booking.getId(), e.getMessage());
        }
    }
    /**
     * Send payment status notification to admin
     */
    @Async
    public void sendPaymentNotification(Booking booking, PaymentStatus paymentStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);

            String emoji = paymentStatus == PaymentStatus.COMPLETED ? "\u2705" : "\u274C";
            helper.setSubject(emoji + " Payment " + paymentStatus.name() + " - Booking #" + booking.getId() + " (" + booking.getCustomerName() + ")");

            String paymentMethodText = booking.getPaymentMethod() != null ? booking.getPaymentMethod() : "N/A";
            String transactionId = booking.getPesapalTransactionId() != null ? booking.getPesapalTransactionId() : "N/A";

            String html = String.format("""
                <!DOCTYPE html>
                <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: %s; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">%s Payment %s</h1>
                    <p style="color: rgba(255,255,255,0.8); margin: 10px 0 0 0;">Booking #%d - %s</p>
                </div>
                <div style="background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; border-top: none; border-radius: 0 0 10px 10px;">
                    <table style="width: 100%%;">
                        <tr><td style="padding: 8px 0; color: #6b7280;">Customer:</td><td style="font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #6b7280;">Phone:</td><td style="font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #6b7280;">Amount:</td><td style="font-weight: bold; color: #059669;">UGX %s</td></tr>
                        <tr><td style="padding: 8px 0; color: #6b7280;">Payment Method:</td><td style="font-weight: bold;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #6b7280;">Transaction ID:</td><td style="font-weight: bold; font-family: monospace;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #6b7280;">Receipt #:</td><td style="font-weight: bold; font-family: monospace;">%s</td></tr>
                    </table>
                </div>
                </body></html>
                """,
                paymentStatus == PaymentStatus.COMPLETED ? "linear-gradient(135deg, #10b981 0%%, #059669 100%%)" : "linear-gradient(135deg, #ef4444 0%%, #dc2626 100%%)",
                emoji,
                paymentStatus.name(),
                booking.getId(),
                booking.getCustomerName(),
                booking.getCustomerName(),
                booking.getPhone(),
                currencyFormat.format(booking.getTotalAmount()),
                paymentMethodText,
                transactionId,
                booking.getReceiptNumber()
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Payment {} notification sent for booking #{}", paymentStatus, booking.getId());
        } catch (Exception e) {
            log.error("Failed to send payment notification for booking #{}: {}", booking.getId(), e.getMessage());
        }
    }
    private String buildAdminNewBookingEmail(Booking booking) {
        StringBuilder itemsHtml = new StringBuilder();
        for (BookingItem item : booking.getItems()) {
            itemsHtml.append(String.format("""
                <tr>
                    <td style="padding: 10px; border-bottom: 1px solid #e5e7eb;">%s</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: center;">%d</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: right;">UGX %s</td>
                    <td style="padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: right;">UGX %s</td>
                </tr>
                """,
                item.getChick().getBreed(),
                item.getQuantity(),
                currencyFormat.format(item.getUnitPrice()),
                currencyFormat.format(item.getSubtotal())
            ));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">🐣 New Booking Received!</h1>
                    <p style="color: #ecfdf5; margin: 10px 0 0 0;">Booking #%d</p>
                </div>
                
                <div style="background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; border-top: none;">
                    <h2 style="color: #059669; margin-top: 0;">Customer Details</h2>
                    <table style="width: 100%%; margin-bottom: 20px;">
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Name:</td>
                            <td style="padding: 8px 0; font-weight: bold;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Phone:</td>
                            <td style="padding: 8px 0; font-weight: bold;">
                                <a href="tel:%s" style="color: #059669;">%s</a>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Location:</td>
                            <td style="padding: 8px 0; font-weight: bold;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Pickup Date:</td>
                            <td style="padding: 8px 0; font-weight: bold;">%s</td>
                        </tr>
                        %s
                    </table>
                    
                    <h2 style="color: #059669;">Order Items</h2>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px;">
                        <thead>
                            <tr style="background: #059669; color: white;">
                                <th style="padding: 12px; text-align: left;">Item</th>
                                <th style="padding: 12px; text-align: center;">Qty</th>
                                <th style="padding: 12px; text-align: right;">Price</th>
                                <th style="padding: 12px; text-align: right;">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                        <tfoot>
                            <tr style="background: #ecfdf5;">
                                <td colspan="3" style="padding: 15px; font-weight: bold; text-align: right;">Grand Total:</td>
                                <td style="padding: 15px; font-weight: bold; text-align: right; color: #059669; font-size: 18px;">UGX %s</td>
                            </tr>
                        </tfoot>
                    </table>
                    
                    %s
                    
                    <div style="text-align: center; margin-top: 30px;">
                        <p style="color: #6b7280; font-size: 14px;">
                            View and manage this booking in your admin dashboard
                        </p>
                    </div>
                </div>
                
                <div style="background: #1f2937; padding: 20px; border-radius: 0 0 10px 10px; text-align: center;">
                    <p style="color: #9ca3af; margin: 0; font-size: 14px;">
                        Suubi Agrovet LTD - Quality Day Old Chicks
                    </p>
                </div>
            </body>
            </html>
            """,
            booking.getId(),
            booking.getCustomerName(),
            booking.getPhone(),
            booking.getPhone(),
            booking.getLocation(),
            booking.getPickupDate().format(dateFormatter),
            buildGpsInfo(booking),
            itemsHtml.toString(),
            currencyFormat.format(booking.getTotalAmount()),
            booking.getNotes() != null && !booking.getNotes().isEmpty() 
                ? "<h3 style=\"color: #6b7280;\">Customer Notes:</h3><p style=\"background: white; padding: 15px; border-radius: 8px; border: 1px solid #e5e7eb;\">" + booking.getNotes() + "</p>" 
                : ""
        );
    }

    private String buildGpsInfo(Booking booking) {
        if (booking.getLatitude() != null && booking.getLongitude() != null) {
            String mapsUrl = String.format("https://www.google.com/maps?q=%f,%f", 
                booking.getLatitude(), booking.getLongitude());
            return String.format("""
                <tr>
                    <td style="padding: 8px 0; color: #6b7280;">GPS Location:</td>
                    <td style="padding: 8px 0;">
                        <a href="%s" target="_blank" style="color: #059669; font-weight: bold;">
                            📍 View on Google Maps
                        </a>
                    </td>
                </tr>
                """, mapsUrl);
        }
        return "";
    }

    private String buildStatusChangeEmail(Booking booking, BookingStatus oldStatus) {
        String statusColor = switch (booking.getStatus()) {
            case PENDING -> "#f59e0b";
            case CONFIRMED -> "#3b82f6";
            case COMPLETED -> "#10b981";
            case CANCELLED -> "#ef4444";
        };

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: %s; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">📋 Booking Status Updated</h1>
                    <p style="color: rgba(255,255,255,0.9); margin: 10px 0 0 0;">Booking #%d</p>
                </div>
                
                <div style="background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; border-top: none;">
                    <div style="text-align: center; margin-bottom: 20px;">
                        <span style="background: #fee2e2; color: #991b1b; padding: 8px 16px; border-radius: 20px; font-weight: bold;">%s</span>
                        <span style="margin: 0 10px;">→</span>
                        <span style="background: %s; color: white; padding: 8px 16px; border-radius: 20px; font-weight: bold;">%s</span>
                    </div>
                    
                    <table style="width: 100%%;">
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Customer:</td>
                            <td style="padding: 8px 0; font-weight: bold;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Phone:</td>
                            <td style="padding: 8px 0;"><a href="tel:%s" style="color: #059669;">%s</a></td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #6b7280;">Amount:</td>
                            <td style="padding: 8px 0; font-weight: bold;">UGX %s</td>
                        </tr>
                    </table>
                </div>
                
                <div style="background: #1f2937; padding: 20px; border-radius: 0 0 10px 10px; text-align: center;">
                    <p style="color: #9ca3af; margin: 0; font-size: 14px;">
                        Suubi Agrovet LTD - Quality Day Old Chicks
                    </p>
                </div>
            </body>
            </html>
            """,
            statusColor,
            booking.getId(),
            oldStatus.name(),
            statusColor,
            booking.getStatus().name(),
            booking.getCustomerName(),
            booking.getPhone(),
            booking.getPhone(),
            currencyFormat.format(booking.getTotalAmount())
        );
    }

    private String buildCustomerReceiptEmail(Booking booking) {
        StringBuilder itemsHtml = new StringBuilder();
        int totalQuantity = 0;
        
        for (BookingItem item : booking.getItems()) {
            totalQuantity += item.getQuantity();
            itemsHtml.append(String.format("""
                <tr>
                    <td style="padding: 12px; border-bottom: 1px solid #e5e7eb;">%s</td>
                    <td style="padding: 12px; border-bottom: 1px solid #e5e7eb; text-align: center;">%d</td>
                    <td style="padding: 12px; border-bottom: 1px solid #e5e7eb; text-align: right;">UGX %s</td>
                    <td style="padding: 12px; border-bottom: 1px solid #e5e7eb; text-align: right;">UGX %s</td>
                </tr>
                """,
                item.getChick().getBreed(),
                item.getQuantity(),
                currencyFormat.format(item.getUnitPrice()),
                currencyFormat.format(item.getSubtotal())
            ));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">🐣 Thank You for Your Order!</h1>
                    <p style="color: #ecfdf5; margin: 10px 0 0 0;">Booking #%d</p>
                </div>
                
                <div style="background: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; border-top: none;">
                    <p style="font-size: 16px; color: #374151;">
                        Dear <strong>%s</strong>,
                    </p>
                    <p style="color: #6b7280;">
                        Thank you for choosing Suubi Agrovet LTD! We have received your order and it is being processed.
                        Here are your booking details:
                    </p>
                    
                    <div style="background: white; border-radius: 8px; padding: 20px; margin: 20px 0; border: 1px solid #e5e7eb;">
                        <h3 style="color: #059669; margin-top: 0;">📋 Booking Information</h3>
                        <table style="width: 100%%;">
                            <tr>
                                <td style="padding: 8px 0; color: #6b7280;">Booking ID:</td>
                                <td style="padding: 8px 0; font-weight: bold;">#%d</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #6b7280;">Status:</td>
                                <td style="padding: 8px 0;">
                                    <span style="background: #fef3c7; color: #92400e; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold;">PENDING</span>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #6b7280;">Pickup Date:</td>
                                <td style="padding: 8px 0; font-weight: bold;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #6b7280;">Delivery Location:</td>
                                <td style="padding: 8px 0; font-weight: bold;">%s</td>
                            </tr>
                        </table>
                    </div>
                    
                    <h3 style="color: #059669;">🐥 Your Order</h3>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px; background: white; border-radius: 8px; overflow: hidden;">
                        <thead>
                            <tr style="background: #059669; color: white;">
                                <th style="padding: 12px; text-align: left;">Item</th>
                                <th style="padding: 12px; text-align: center;">Qty</th>
                                <th style="padding: 12px; text-align: right;">Price</th>
                                <th style="padding: 12px; text-align: right;">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    
                    <div style="background: #ecfdf5; border-radius: 8px; padding: 20px; text-align: right;">
                        <p style="margin: 0; color: #6b7280;">Total Chicks: <strong>%d</strong></p>
                        <p style="margin: 10px 0 0 0; font-size: 24px; color: #059669; font-weight: bold;">
                            Total: UGX %s
                        </p>
                    </div>
                    
                    <div style="background: #fef3c7; border-radius: 8px; padding: 15px; margin-top: 20px;">
                        <p style="margin: 0; color: #92400e; font-size: 14px;">
                            <strong>📞 What's Next?</strong><br>
                            Our team will contact you on <strong>%s</strong> to confirm your order and arrange delivery details.
                        </p>
                    </div>
                    
                    %s
                </div>
                
                <div style="background: #1f2937; padding: 20px; border-radius: 0 0 10px 10px; text-align: center;">
                    <p style="color: white; margin: 0 0 10px 0; font-weight: bold;">Suubi Agrovet LTD</p>
                    <p style="color: #9ca3af; margin: 0; font-size: 14px;">
                        Quality Day Old Chicks<br>
                        Questions? Reply to this email or call us.
                    </p>
                </div>
            </body>
            </html>
            """,
            booking.getId(),
            booking.getCustomerName(),
            booking.getId(),
            booking.getPickupDate().format(dateFormatter),
            booking.getLocation(),
            itemsHtml.toString(),
            totalQuantity,
            currencyFormat.format(booking.getTotalAmount()),
            booking.getPhone(),
            booking.getNotes() != null && !booking.getNotes().isEmpty() 
                ? "<p style=\"color: #6b7280; font-size: 14px;\"><strong>Your Notes:</strong> " + booking.getNotes() + "</p>" 
                : ""
        );
    }
}
