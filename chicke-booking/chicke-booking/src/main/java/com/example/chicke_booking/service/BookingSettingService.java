package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.BookingSetting;
import com.example.chicke_booking.repository.BookingSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingSettingService {

    private static final String BOOKING_SETTING_KEY = "MAIN_BOOKING_SETTING";
    
    private final BookingSettingRepository bookingSettingRepository;

    public Optional<BookingSetting> getBookingSetting() {
        return bookingSettingRepository.findByKey(BOOKING_SETTING_KEY);
    }

    public LocalDate getNextBookingDate() {
        return getBookingSetting()
                .map(BookingSetting::getNextBookingDate)
                .orElse(null);
    }

    public boolean isBookingEnabled() {
        return getBookingSetting()
                .map(BookingSetting::isBookingEnabled)
                .orElse(true);
    }

    public String getBookingMessage() {
        return getBookingSetting()
                .map(BookingSetting::getMessage)
                .orElse(null);
    }

    @Transactional
    public BookingSetting updateNextBookingDate(LocalDate nextBookingDate, boolean bookingEnabled, 
                                                  String message, String updatedBy) {
        BookingSetting setting = bookingSettingRepository.findByKey(BOOKING_SETTING_KEY)
                .orElse(BookingSetting.builder()
                        .key(BOOKING_SETTING_KEY)
                        .build());

        setting.setNextBookingDate(nextBookingDate);
        setting.setBookingEnabled(bookingEnabled);
        setting.setMessage(message);
        setting.setUpdatedBy(updatedBy);

        return bookingSettingRepository.save(setting);
    }

    @Transactional
    public BookingSetting setNextBookingDate(LocalDate nextBookingDate, String updatedBy) {
        BookingSetting setting = bookingSettingRepository.findByKey(BOOKING_SETTING_KEY)
                .orElse(BookingSetting.builder()
                        .key(BOOKING_SETTING_KEY)
                        .bookingEnabled(true)
                        .build());

        setting.setNextBookingDate(nextBookingDate);
        setting.setUpdatedBy(updatedBy);

        return bookingSettingRepository.save(setting);
    }

    @Transactional
    public void toggleBookingEnabled(boolean enabled, String updatedBy) {
        BookingSetting setting = bookingSettingRepository.findByKey(BOOKING_SETTING_KEY)
                .orElse(BookingSetting.builder()
                        .key(BOOKING_SETTING_KEY)
                        .build());

        setting.setBookingEnabled(enabled);
        setting.setUpdatedBy(updatedBy);

        bookingSettingRepository.save(setting);
    }
}
