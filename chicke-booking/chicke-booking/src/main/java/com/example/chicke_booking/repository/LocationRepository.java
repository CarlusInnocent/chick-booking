package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    
    List<Location> findByActiveTrue();
    
    List<Location> findByActiveTrueOrderByNameAsc();
    
    List<Location> findByDistrictContainingIgnoreCase(String district);
    
    List<Location> findByRegionContainingIgnoreCase(String region);
    
    List<Location> findByNameContainingIgnoreCaseOrDistrictContainingIgnoreCase(String name, String district);
    
    boolean existsByNameIgnoreCase(String name);
}
