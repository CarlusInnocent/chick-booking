package com.example.chicke_booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChickeBookingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChickeBookingApplication.class, args);
	}

}
