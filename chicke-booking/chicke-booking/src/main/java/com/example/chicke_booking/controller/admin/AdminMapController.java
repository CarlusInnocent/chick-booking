package com.example.chicke_booking.controller.admin;

import com.example.chicke_booking.model.entity.Booking;
import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.model.enums.BookingStatus;
import com.example.chicke_booking.repository.BookingRepository;
import com.example.chicke_booking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/map")
@PreAuthorize("hasAnyRole('DEVELOPER', 'ADMIN', 'OPERATOR')")
@RequiredArgsConstructor
public class AdminMapController {

    private final BookingRepository bookingRepository;
    private final LocationService locationService;

    @GetMapping
    public String showMap(Model model,
                          @RequestParam(required = false) String status) {
        List<Location> locations = locationService.getActiveLocations();
        model.addAttribute("locations", locations);
        model.addAttribute("statusFilter", status);
        return "admin/map/index";
    }

    @GetMapping("/api/bookings")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getBookingsForMap(
            @RequestParam(required = false) String status) {
        
        List<Booking> bookings;
        if (status != null && !status.isEmpty()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingRepository.findByStatusWithCoordinates(bookingStatus);
            } catch (IllegalArgumentException e) {
                bookings = bookingRepository.findAllWithCoordinates();
            }
        } else {
            bookings = bookingRepository.findAllWithCoordinates();
        }

        List<Map<String, Object>> markers = bookings.stream()
                .map(this::bookingToMarker)
                .collect(Collectors.toList());

        return ResponseEntity.ok(markers);
    }

    @GetMapping("/api/locations")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getLocationsForMap() {
        List<Location> locations = locationService.getActiveLocations();
        
        List<Map<String, Object>> markers = locations.stream()
                .map(this::locationToMarker)
                .collect(Collectors.toList());

        return ResponseEntity.ok(markers);
    }

    private Map<String, Object> bookingToMarker(Booking booking) {
        Map<String, Object> marker = new HashMap<>();
        marker.put("id", booking.getId());
        marker.put("lat", booking.getLatitude());
        marker.put("lng", booking.getLongitude());
        marker.put("customerName", booking.getCustomerName());
        marker.put("phone", booking.getPhone());
        marker.put("location", booking.getLocation());
        marker.put("status", booking.getStatus().name());
        marker.put("pickupDate", booking.getPickupDate().toString());
        marker.put("totalAmount", booking.getTotalAmount());
        marker.put("type", "booking");
        return marker;
    }

    private Map<String, Object> locationToMarker(Location location) {
        Map<String, Object> marker = new HashMap<>();
        marker.put("id", location.getId());
        marker.put("lat", location.getLatitude());
        marker.put("lng", location.getLongitude());
        marker.put("name", location.getName());
        marker.put("district", location.getDistrict());
        marker.put("region", location.getRegion());
        marker.put("contact", location.getContact());
        marker.put("type", "location");
        return marker;
    }
}
