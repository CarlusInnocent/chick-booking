package com.example.chicke_booking.repository;

import com.example.chicke_booking.model.entity.User;
import com.example.chicke_booking.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    List<User> findByRole(Role role);
    
    List<User> findByRoleIn(List<Role> roles);
}
