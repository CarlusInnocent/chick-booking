package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.BookingItem;
import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.repository.BookingRepository;
import com.example.chicke_booking.repository.ChickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ChickRepository chickRepository;
    private final EmailService emailService;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllOrderByCreatedAtDesc();
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> searchByCustomerName(String name) {
        return bookingRepository.findByCustomerNameContainingIgnoreCase(name);
    }

    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByPickupDateBetween(startDate, endDate);
    }

    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    public long getTotalBookingsCount() {
        return bookingRepository.count();
    }

    @Transactional
    public Booking createBooking(String customerName, String location, String phone,
                                  LocalDate pickupDate, String notes, Map<Long, Integer> chickQuantities) {
        return createBooking(customerName, location, phone, null, pickupDate, notes, chickQuantities, null, null);
    }

    @Transactional
    public Booking createBooking(String customerName, String location, String phone, String email,
                                  LocalDate pickupDate, String notes, Map<Long, Integer> chickQuantities,
                                  Double latitude, Double longitude) {
        
        // Clean email - trim whitespace and treat empty as null
        String cleanEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;
        
        Booking booking = Booking.builder()
                .customerName(customerName)
                .location(location)
                .phone(phone)
                .email(cleanEmail)
                .pickupDate(pickupDate)
                .status(BookingStatus.PENDING)
                .notes(notes)
                .latitude(latitude)
                .longitude(longitude)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : chickQuantities.entrySet()) {
            Long chickId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity != null && quantity > 0) {
                Chick chick = chickRepository.findById(chickId)
                        .orElseThrow(() -> new IllegalArgumentException("Chick not found: " + chickId));

                BookingItem item = BookingItem.builder()
                        .chick(chick)
                        .quantity(quantity)
                        .unitPrice(chick.getPrice())
                        .build();

                booking.addItem(item);
                totalAmount = totalAmount.add(item.getSubtotal());
            }
        }

        booking.setTotalAmount(totalAmount);

        Booking savedBooking = bookingRepository.save(booking);
        
        // Force load items and chicks for async email (avoid lazy loading issues)
        savedBooking.getItems().forEach(item -> item.getChick().getBreed());
        
        // Send email notification asynchronously
        emailService.sendNewBookingNotification(savedBooking);
        
        return savedBooking;
    }

    @Transactional
    public Booking updateBookingStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        Booking savedBooking = bookingRepository.save(booking);
        
        // Send status change notification
        if (oldStatus != status) {
            emailService.sendStatusChangeNotification(savedBooking, oldStatus);
        }
        
        return savedBooking;
    }

    @Transactional
    public Booking updateBooking(Long id, String customerName, String location, String phone,
                                  LocalDate pickupDate, String notes, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));

        BookingStatus oldStatus = booking.getStatus();
        
        booking.setCustomerName(customerName);
        booking.setLocation(location);
        booking.setPhone(phone);
        booking.setPickupDate(pickupDate);
        booking.setNotes(notes);
        booking.setStatus(status);

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send status change notification if status changed
        if (oldStatus != status) {
            emailService.sendStatusChangeNotification(savedBooking, oldStatus);
        }
        
        return savedBooking;
    }

    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public BigDecimal getTotalRevenue() {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Booking> getRecentBookings(int limit) {
        List<Booking> allBookings = bookingRepository.findAllOrderByCreatedAtDesc();
        return allBookings.stream().limit(limit).toList();
    }
}
