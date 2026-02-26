package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByStatus(BookingStatus status);
    
    List<Booking> findByCustomerNameContainingIgnoreCase(String customerName);
    
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
}
