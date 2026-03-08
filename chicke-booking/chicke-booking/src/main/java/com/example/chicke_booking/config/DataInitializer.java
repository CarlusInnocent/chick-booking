package com.example.chicke_booking.config;

import com.example.chicke_booking.model.entity.Chick;
import com.example.chicke_booking.model.entity.User;
import com.example.chicke_booking.model.enums.Role;
import com.example.chicke_booking.repository.ChickRepository;
import com.example.chicke_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ChickRepository chickRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initializeUsers();
        initializeChicks();
    }

    private void initializeUsers() {
        // Create Developer account
        if (!userRepository.existsByUsername("developer")) {
            User developer = User.builder()
                    .username("developer")
                    .password(passwordEncoder.encode("dev123"))
                    .fullName("Developer Account")
                    .role(Role.DEVELOPER)
                    .enabled(true)
                    .createdBy("system")
                    .build();
            userRepository.save(developer);
            log.info("Created developer account: developer");
        }

        // Create Admin account
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Admin Account")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .createdBy("system")
                    .build();
            userRepository.save(admin);
            log.info("Created admin account: admin");
        }
    }

    private void initializeChicks() {
        if (chickRepository.count() == 0) {
            // Add sample chick breeds
            chickRepository.save(Chick.builder()
                    .breed("Rhode Island Red")
                    .description("Excellent layers producing brown eggs. Hardy and friendly birds.")
                    .price(new BigDecimal("3.50"))
                    .active(true)
                    .build());

            chickRepository.save(Chick.builder()
                    .breed("Leghorn")
                    .description("Prolific white egg layers. Active and alert birds.")
                    .price(new BigDecimal("2.75"))
                    .active(true)
                    .build());

            chickRepository.save(Chick.builder()
                    .breed("Plymouth Rock")
                    .description("Dual-purpose breed good for eggs and meat. Docile temperament.")
                    .price(new BigDecimal("4.00"))
                    .active(true)
                    .build());

            chickRepository.save(Chick.builder()
                    .breed("Silkie")
                    .description("Ornamental breed with fluffy plumage. Great pets and brooders.")
                    .price(new BigDecimal("5.50"))
                    .active(true)
                    .build());

            chickRepository.save(Chick.builder()
                    .breed("Australorp")
                    .description("Record-breaking egg layers. Calm and adaptable to various climates.")
                    .price(new BigDecimal("3.75"))
                    .active(true)
                    .build());

            chickRepository.save(Chick.builder()
                    .breed("Orpington")
                    .description("Large, friendly birds. Good layers and excellent meat quality.")
                    .price(new BigDecimal("4.25"))
                    .active(true)
                    .build());

            log.info("Initialized sample chick breeds");
        }
    }
}
