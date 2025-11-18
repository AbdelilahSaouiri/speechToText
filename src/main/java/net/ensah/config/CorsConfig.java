package net.ensah.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Autoriser toutes les origines (pour le développement)
        // En production, spécifiez les domaines autorisés
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:8081",
            "http://localhost:8080",
            "http://127.0.0.1:8081",
            "http://127.0.0.1:8080",
            "*" // Pour permettre l'accès depuis n'importe quelle origine
        ));
        
        // Autoriser les méthodes HTTP
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Autoriser les en-têtes
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // Autoriser l'envoi de credentials (cookies, etc.)
        config.setAllowCredentials(true);
        
        // Autoriser les en-têtes exposés au client
        config.setExposedHeaders(Arrays.asList("Content-Type", "Content-Length", "Content-Disposition"));
        
        // Durée de mise en cache de la pré-requête OPTIONS
        config.setMaxAge(3600L);
        
        // Appliquer la configuration à tous les chemins
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}

