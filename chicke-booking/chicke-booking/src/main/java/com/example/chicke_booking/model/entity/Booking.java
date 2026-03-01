package com.example.chicke_booking.model.entity;

import com.example.chicke_booking.model.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique receipt number for searching (nullable for existing records)
    @Column(unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String location;

    // Customer's GPS coordinates for map tracking
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    // Optional reference to a managed location
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location locationRef;

    @Column(nullable = false)
    private String phone;

    // Optional customer email for receipt
    @Column
    private String email;

    @Column(nullable = false)
    private LocalDate pickupDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Generate receipt number if not set
        if (receiptNumber == null || receiptNumber.isEmpty()) {
            // Format: CHK-YYYYMMDD-XXXX (e.g., CHK-20260301-A1B2)
            String datePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            String uniquePart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            receiptNumber = "CHK-" + datePart + "-" + uniquePart;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(BookingItem item) {
        items.add(item);
        item.setBooking(this);
    }

    public void removeItem(BookingItem item) {
        items.remove(item);
        item.setBooking(null);
    }
}
