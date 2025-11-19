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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * @param onOpen Callback appelé quand la connexion est établie
     * @return WebSocketClient configuré
     */
    public WebSocketClient createConnection(Consumer<String> onMessage, Consumer<Exception> onError, Runnable onOpen) {
        try {
            // Vérifier que la clé API est définie
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("❌ DEEPGRAM_API_KEY n'est pas définie!");
                throw new RuntimeException("DEEPGRAM_API_KEY n'est pas définie. Définissez la variable d'environnement DEEPGRAM_API_KEY.");
            }
            
            // Log partiel de la clé pour vérification (premiers et derniers caractères)
            if (apiKey.length() > 10) {
                String maskedKey = apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
                logger.info("🔑 Clé API Deepgram chargée (masquée): {}", maskedKey);
            } else {
                logger.warn("⚠️ Clé API Deepgram semble trop courte: {} caractères", apiKey.length());
            }
            
            // Construire l'URL avec les paramètres de transcription
            // Utiliser URIBuilder pour un encodage correct, ou construire manuellement
            // Essayer d'abord avec les paramètres minimaux
            String url = DEEPGRAM_WS_URL 
                    + "?token=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                    + "&model=nova-2"
                    + "&language=fr"
                    + "&encoding=linear16"
                    + "&sample_rate=16000"
                    + "&channels=1"
                    + "&interim_results=true";
            
            logger.info("🔗 Connexion à Deepgram");
            logger.info("🔗 URL (token masqué): {}", url.replace(apiKey, "***"));
            logger.info("🔗 Longueur clé API: {} caractères", apiKey.length());
            logger.info("🔗 Longueur URL totale: {} caractères", url.length());
            
            // Créer l'URI - cela devrait encoder correctement
            URI serverUri = new URI(url);
            
            // Vérifier l'URI créé
            logger.debug("🔗 URI créé - Scheme: {}, Host: {}, Path: {}, Query: {}", 
                    serverUri.getScheme(), serverUri.getHost(), serverUri.getPath(), serverUri.getQuery());
            
            WebSocketClient client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("✅ Connexion Deepgram établie - Status: {}", handshake.getHttpStatus());
                    if (onOpen != null) {
                        onOpen.run();
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        logger.debug("📨 Message Deepgram reçu: {}", message);
                        JsonNode json = objectMapper.readTree(message);
                        
                        // Vérifier s'il y a une erreur
                        if (json.has("error")) {
                            String errorMsg = json.get("error").asText();
                            logger.error("❌ Erreur Deepgram: {}", errorMsg);
                            onError.accept(new Exception("Deepgram error: " + errorMsg));
                            return;
                        }
                        
                        // Vérifier le type de message
                        String messageType = json.has("type") ? json.get("type").asText() : "";
                        
                        // Messages de métadonnées (ignorés)
                        if ("Metadata".equals(messageType) || "SpeechStarted".equals(messageType)) {
                            logger.debug("📋 Message métadonnées Deepgram: {}", messageType);
                            return;
                        }
                        
                        // Extraire la transcription depuis la structure Deepgram
                        // Structure: {"channel_index": [0], "duration": ..., "start": ..., "is_final": ..., "channel": {"alternatives": [...]}}
                        boolean isFinal = json.has("is_final") && json.get("is_final").asBoolean();
                        String transcript = "";
                        
                        if (json.has("channel")) {
                            JsonNode channel = json.get("channel");
                            if (channel.has("alternatives")) {
                                JsonNode alternatives = channel.get("alternatives");
                                if (alternatives.isArray() && alternatives.size() > 0) {
                                    JsonNode firstAlt = alternatives.get(0);
                                    if (firstAlt.has("transcript")) {
                                        transcript = firstAlt.get("transcript").asText();
                                    }
                                }
                            }
                        }
                        
                        // Si pas de transcript dans channel, essayer directement dans le JSON
                        if (transcript.isEmpty() && json.has("transcript")) {
                            transcript = json.get("transcript").asText();
                        }
                        
                        // Envoyer la transcription si elle n'est pas vide
                        if (!transcript.isEmpty()) {
                            String prefix = isFinal ? "[FINAL]" : "[INTERIM]";
                            logger.info("📝 Transcription Deepgram ({}): {}", isFinal ? "FINAL" : "INTERIM", transcript);
                            onMessage.accept(prefix + transcript);
                        } else {
                            logger.debug("⚠️ Message Deepgram sans transcription: {}", message);
                        }
                    } catch (Exception e) {
                        logger.error("❌ Erreur parsing message Deepgram: {} - Message: {}", e.getMessage(), message);
                        // Ne pas appeler onError pour les erreurs de parsing, juste logger
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("🔌 Connexion Deepgram fermée: {} - {} (remote: {})", code, reason, remote);
                    if (code != 1000) { // 1000 = fermeture normale
                        logger.error("❌ Connexion fermée avec code d'erreur: {} - {}", code, reason);
                        onError.accept(new Exception("Connexion Deepgram fermée: " + code + " - " + reason));
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    logger.error("❌ Erreur WebSocket Deepgram: {} - {}", ex.getMessage(), ex.getClass().getName());
                    if (ex.getCause() != null) {
                        logger.error("❌ Cause: {}", ex.getCause().getMessage());
                    }
                    onError.accept(ex);
                }
            };
            
            // Ajouter l'en-tête Authorization (méthode principale)
            // Java-WebSocket devrait envoyer cet en-tête lors du handshake
            try {
                client.addHeader("Authorization", "Token " + apiKey);
                logger.info("✅ En-tête Authorization ajouté: Token " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
            } catch (Exception e) {
                logger.error("❌ Impossible d'ajouter l'en-tête Authorization: {}", e.getMessage());
                throw new RuntimeException("Impossible d'ajouter l'en-tête Authorization", e);
            }
            
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
            try {
                client.send(audioData);
                logger.debug("✅ Audio envoyé à Deepgram: {} bytes", audioData.length);
            } catch (Exception e) {
                logger.error("❌ Erreur envoi audio à Deepgram: {}", e.getMessage());
            }
        } else {
            logger.warn("⚠️ Client Deepgram non connecté (client={}, isOpen={})", 
                client != null, client != null && client.isOpen());
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

