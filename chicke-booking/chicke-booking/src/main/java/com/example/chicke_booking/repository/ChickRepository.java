package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.Chick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChickRepository extends JpaRepository<Chick, Long> {
    
    List<Chick> findByActiveTrue();
    
    List<Chick> findByBreedContainingIgnoreCase(String breed);
}
