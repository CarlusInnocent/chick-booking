package com.example.chicke_booking.controller;

import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicApiController {

    private final LocationService locationService;

    /**
     * Get all active store/delivery locations for public display
     */
    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getPublicLocations() {
        List<Location> locations = locationService.getActiveLocations();
        
        List<Map<String, Object>> result = locations.stream()
                .map(this::locationToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> locationToMap(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", location.getId());
        map.put("name", location.getName());
        map.put("district", location.getDistrict());
        map.put("region", location.getRegion());
        map.put("description", location.getDescription());
        map.put("lat", location.getLatitude());
        map.put("lng", location.getLongitude());
        map.put("deliveryFee", location.getDeliveryFee());
        return map;
    }
}
