package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.BookingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingSettingRepository extends JpaRepository<BookingSetting, Long> {
    
    Optional<BookingSetting> findByKey(String key);
}
