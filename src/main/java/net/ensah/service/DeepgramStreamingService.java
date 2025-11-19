package net.ensah.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.function.Consumer;

@Service
public class DeepgramStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepgramStreamingService.class);
    private static final String DEEPGRAM_WS_URL = "wss://api.deepgram.com/v1/listen";
    
    @Value("${deepgram.api.key}")
    private String apiKey;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Crée une connexion WebSocket vers Deepgram et retourne le client
     * @param onMessage Callback pour les messages de transcription
     * @param onError Callback pour les erreurs
     * @return WebSocketClient configuré
     */
    public WebSocketClient createConnection(Consumer<String> onMessage, Consumer<Exception> onError) {
        try {
            // Construire l'URL avec les paramètres de transcription
            String url = DEEPGRAM_WS_URL + "?model=nova-2&language=fr&interim_results=true&punctuate=true&smart_format=true";
            
            URI serverUri = new URI(url);
            
            WebSocketClient client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("✅ Connexion Deepgram établie");
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        JsonNode json = objectMapper.readTree(message);
                        
                        // Vérifier s'il y a une erreur
                        if (json.has("error")) {
                            String errorMsg = json.get("error").asText();
                            logger.error("❌ Erreur Deepgram: {}", errorMsg);
                            onError.accept(new Exception("Deepgram error: " + errorMsg));
                            return;
                        }
                        
                        // Extraire la transcription
                        if (json.has("channel")) {
                            JsonNode channel = json.get("channel");
                            if (channel.has("alternatives")) {
                                JsonNode alternatives = channel.get("alternatives");
                                if (alternatives.isArray() && alternatives.size() > 0) {
                                    JsonNode firstAlt = alternatives.get(0);
                                    if (firstAlt.has("transcript")) {
                                        String transcript = firstAlt.get("transcript").asText();
                                        boolean isFinal = json.has("is_final") && json.get("is_final").asBoolean();
                                        
                                        if (!transcript.isEmpty()) {
                                            String prefix = isFinal ? "[FINAL]" : "[INTERIM]";
                                            onMessage.accept(prefix + transcript);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("❌ Erreur parsing message Deepgram: {}", e.getMessage());
                        onError.accept(e);
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("🔌 Connexion Deepgram fermée: {} - {}", code, reason);
                }
                
                @Override
                public void onError(Exception ex) {
                    logger.error("❌ Erreur WebSocket Deepgram: {}", ex.getMessage());
                    onError.accept(ex);
                }
            };
            
            // Ajouter l'en-tête d'authentification
            client.addHeader("Authorization", "Token " + apiKey);
            
            return client;
            
        } catch (Exception e) {
            logger.error("❌ Erreur création connexion Deepgram: {}", e.getMessage());
            throw new RuntimeException("Impossible de créer la connexion Deepgram", e);
        }
    }
    
    /**
     * Envoie des données audio à Deepgram
     * @param client Le client WebSocket
     * @param audioData Les données audio en bytes
     */
    public void sendAudioData(WebSocketClient client, byte[] audioData) {
        if (client != null && client.isOpen()) {
            client.send(audioData);
        } else {
            logger.warn("⚠️ Client Deepgram non connecté");
        }
    }
    
    /**
     * Ferme la connexion Deepgram
     * @param client Le client WebSocket
     */
    public void closeConnection(WebSocketClient client) {
        if (client != null && client.isOpen()) {
            // Envoyer un message de fin de stream
            try {
                String closeMessage = "{\"type\":\"CloseStream\"}";
                client.send(closeMessage);
            } catch (Exception e) {
                logger.warn("⚠️ Erreur lors de l'envoi du message de fermeture: {}", e.getMessage());
            }
            client.close();
        }
    }
}

