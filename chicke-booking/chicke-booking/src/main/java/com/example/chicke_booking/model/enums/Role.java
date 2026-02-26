package com.example.chicke_booking.model.enums;

public enum Role {
    DEVELOPER,  // Can do everything
    ADMIN,      // Can CRUD bookings + manage operators
    OPERATOR    // Can Create and Read bookings only
}
