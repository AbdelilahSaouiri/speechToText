package net.ensah.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import net.ensah.web.AudioWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final AudioWebSocketHandler audioWebSocketHandler;
    
    public WebSocketConfig(AudioWebSocketHandler audioWebSocketHandler) {
        this.audioWebSocketHandler = audioWebSocketHandler;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/ws/audio/stream")
                .setAllowedOrigins("*"); // En production, spécifiez les origines autorisées
    }
}

