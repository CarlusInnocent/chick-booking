package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.Location;
import com.example.chicke_booking.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    public List<Location> getActiveLocations() {
        return locationRepository.findByActiveTrueOrderByNameAsc();
    }

    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }

    public List<Location> searchLocations(String query) {
        return locationRepository.findByNameContainingIgnoreCaseOrDistrictContainingIgnoreCase(query, query);
    }

    public List<Location> getLocationsByDistrict(String district) {
        return locationRepository.findByDistrictContainingIgnoreCase(district);
    }

    public List<Location> getLocationsByRegion(String region) {
        return locationRepository.findByRegionContainingIgnoreCase(region);
    }

    public boolean existsByName(String name) {
        return locationRepository.existsByNameIgnoreCase(name);
    }

    @Transactional
    public Location createLocation(String name, String district, String region, String description,
                                    Double latitude, Double longitude, String contact) {
        Location location = Location.builder()
                .name(name)
                .district(district)
                .region(region)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .contact(contact)
                .active(true)
                .build();

        return locationRepository.save(location);
    }

    @Transactional
    public Location updateLocation(Long id, String name, String district, String region, String description,
                                    Double latitude, Double longitude, String contact, boolean active) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));

        location.setName(name);
        location.setDistrict(district);
        location.setRegion(region);
        location.setDescription(description);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setContact(contact);
        location.setActive(active);

        return locationRepository.save(location);
    }

    @Transactional
    public void toggleActive(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        location.setActive(!location.getActive());
        locationRepository.save(location);
    }

    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new IllegalArgumentException("Location not found: " + id);
        }
        locationRepository.deleteById(id);
    }

    public long count() {
        return locationRepository.count();
    }
}
