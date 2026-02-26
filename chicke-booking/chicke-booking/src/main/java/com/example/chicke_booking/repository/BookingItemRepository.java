package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    
    List<BookingItem> findByBookingId(Long bookingId);
    
    List<BookingItem> findByChickId(Long chickId);
}
