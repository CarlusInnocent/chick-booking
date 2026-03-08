package com.example.chicke_booking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationSuccessHandler jwtAuthSuccessHandler;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - Customer pages
                        .requestMatchers("/", "/catalog", "/catalog/**", "/booking/**").permitAll()
                        .requestMatchers("/login", "/error").permitAll()
                        
                        // Payment callback & IPN (PesaPal)
                        .requestMatchers("/payment/**").permitAll()
                        
                        // Public API endpoints
                        .requestMatchers("/api/public/**").permitAll()
                        
                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/logo/**", "/webjars/**", "/favicon.ico").permitAll()
                        
                        // Admin endpoints - Role-based access
                        // Operators can view, create bookings, and change status
                        .requestMatchers("/admin/bookings", "/admin/bookings/new", "/admin/bookings/create").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        .requestMatchers("/admin/bookings/{id}").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        .requestMatchers("/admin/bookings/*/status").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        
                        // Only DEVELOPER and ADMIN can update/delete bookings
                        .requestMatchers("/admin/bookings/*/edit", "/admin/bookings/*/update", "/admin/bookings/*/delete").hasAnyRole("DEVELOPER", "ADMIN")
                        
                        // Only DEVELOPER and ADMIN can manage operators
                        .requestMatchers("/admin/operators/**").hasAnyRole("DEVELOPER", "ADMIN")
                        
                        // Only DEVELOPER and ADMIN can manage chicks
                        .requestMatchers("/admin/chicks/**").hasAnyRole("DEVELOPER", "ADMIN")
                        
                        // Dashboard access for all admin roles
                        .requestMatchers("/admin/dashboard").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        
                        // Booking settings - DEVELOPER, ADMIN, and OPERATOR can update
                        .requestMatchers("/admin/settings/**").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        
                        // All other admin routes require authentication
                        .requestMatchers("/admin/**").hasAnyRole("DEVELOPER", "ADMIN", "OPERATOR")
                        
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(jwtAuthSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies(JwtAuthenticationFilter.JWT_COOKIE_NAME)
                        .permitAll()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
