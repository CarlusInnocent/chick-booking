package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.repository.ChickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChickService {

    private final ChickRepository chickRepository;

    public List<Chick> getAllChicks() {
        return chickRepository.findAll();
    }

    public List<Chick> getActiveChicks() {
        return chickRepository.findByActiveTrue();
    }

    public Optional<Chick> getChickById(Long id) {
        return chickRepository.findById(id);
    }

    public List<Chick> searchByBreed(String breed) {
        return chickRepository.findByBreedContainingIgnoreCase(breed);
    }

    @Transactional
    public Chick createChick(String breed, String description, BigDecimal price, String imageUrl) {
        Chick chick = Chick.builder()
                .breed(breed)
                .description(description)
                .price(price)
                .imageUrl(imageUrl)
                .active(true)
                .build();

        return chickRepository.save(chick);
    }

    @Transactional
    public Chick updateChick(Long id, String breed, String description, BigDecimal price, String imageUrl, boolean active) {
        Chick chick = chickRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chick not found: " + id));

        chick.setBreed(breed);
        chick.setDescription(description);
        chick.setPrice(price);
        chick.setImageUrl(imageUrl);
        chick.setActive(active);

        return chickRepository.save(chick);
    }

    @Transactional
    public void deleteChick(Long id) {
        chickRepository.deleteById(id);
    }

    @Transactional
    public void toggleActive(Long id) {
        Chick chick = chickRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chick not found: " + id));

        chick.setActive(!chick.isActive());
        chickRepository.save(chick);
    }
}
