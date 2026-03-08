package com.example.chicke_booking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pesapal")
@Getter
@Setter
public class PesaPalConfig {

    private String consumerKey;
    private String consumerSecret;
    private String apiUrl;
    private String callbackUrl;
    private String ipnUrl;
}
