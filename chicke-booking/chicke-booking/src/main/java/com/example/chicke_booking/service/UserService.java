package com.example.chicke_booking.service;

import com.example.chicke_booking.model.entity.User;
import com.example.chicke_booking.model.enums.Role;
import com.example.chicke_booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getOperators() {
        return userRepository.findByRole(Role.OPERATOR);
    }

    public List<User> getAdmins() {
        return userRepository.findByRole(Role.ADMIN);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User createUser(String username, String password, String fullName, Role role, String createdBy) {
        if (existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .createdBy(createdBy)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User createOperator(String username, String password, String fullName, String createdBy) {
        return createUser(username, password, fullName, Role.OPERATOR, createdBy);
    }

    @Transactional
    public User createAdmin(String username, String password, String fullName, String createdBy) {
        return createUser(username, password, fullName, Role.ADMIN, createdBy);
    }

    @Transactional
    public User updateUser(Long id, String fullName, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setFullName(fullName);
        user.setEnabled(enabled);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
