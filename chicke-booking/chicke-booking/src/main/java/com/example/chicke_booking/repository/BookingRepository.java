package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByStatus(BookingStatus status);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.items i LEFT JOIN FETCH i.chick WHERE b.id = :id")
    Optional<Booking> findByIdWithItems(Long id);
    
    List<Booking> findByCustomerNameContainingIgnoreCase(String customerName);
    
    // Search by receipt number
    Optional<Booking> findByReceiptNumber(String receiptNumber);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.items i LEFT JOIN FETCH i.chick WHERE b.receiptNumber = :receiptNumber")
    Optional<Booking> findByReceiptNumberWithItems(String receiptNumber);
    
    // Search by receipt number containing (partial match)
    List<Booking> findByReceiptNumberContainingIgnoreCase(String receiptNumber);
    
    List<Booking> findByPickupDate(LocalDate pickupDate);
    
    List<Booking> findByPickupDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(BookingStatus status);
    
    @Query("SELECT b FROM Booking b ORDER BY b.createdAt DESC")
    List<Booking> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.latitude IS NOT NULL AND b.longitude IS NOT NULL")
    List<Booking> findAllWithCoordinates();

    @Query("SELECT b FROM Booking b WHERE b.latitude IS NOT NULL AND b.longitude IS NOT NULL AND b.status = :status")
    List<Booking> findByStatusWithCoordinates(BookingStatus status);

    Optional<Booking> findByPesapalOrderTrackingId(String pesapalOrderTrackingId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.paymentStatus = :paymentStatus")
    long countByPaymentStatus(PaymentStatus paymentStatus);
}
