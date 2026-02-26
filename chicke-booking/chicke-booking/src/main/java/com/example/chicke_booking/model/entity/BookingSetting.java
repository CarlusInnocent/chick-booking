package com.example.chicke_booking.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "next_booking_date")
    private LocalDate nextBookingDate;

    @Column(name = "booking_enabled")
    @Builder.Default
    private boolean bookingEnabled = true;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
