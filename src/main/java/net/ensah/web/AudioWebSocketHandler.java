package net.ensah.web;

import net.ensah.service.DeepgramStreamingService;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioWebSocketHandler.class);
    
    private final DeepgramStreamingService deepgramService;
    private final ConcurrentHashMap<String, WebSocketClient> deepgramClients = new ConcurrentHashMap<>();
    
    public AudioWebSocketHandler(DeepgramStreamingService deepgramService) {
        this.deepgramService = deepgramService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("🔌 Nouvelle connexion WebSocket: {}", session.getId());
        
        // Créer une connexion Deepgram pour cette session
        WebSocketClient deepgramClient = deepgramService.createConnection(
            // Callback pour les messages de transcription
            (transcript) -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(transcript));
                        logger.info("📤 Transcription envoyée au client: {}", transcript);
                    }
                } catch (Exception e) {
                    logger.error("❌ Erreur envoi transcription: {}", e.getMessage());
                }
            },
            // Callback pour les erreurs
            (error) -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("ERROR: " + error.getMessage()));
                    }
                } catch (Exception e) {
                    logger.error("❌ Erreur envoi erreur: {}", e.getMessage());
                }
            },
            // Callback pour onOpen - envoie le message CONNECTED
            () -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("CONNECTED:DEEPGRAM"));
                        logger.info("✅ Connexion Deepgram établie pour session: {}", session.getId());
                    }
                } catch (Exception e) {
                    logger.error("❌ Erreur envoi message CONNECTED: {}", e.getMessage());
                }
            }
        );
        
        // Connecter au serveur Deepgram de manière asynchrone
        deepgramClients.put(session.getId(), deepgramClient);
        
        // Connecter dans un thread séparé pour ne pas bloquer
        new Thread(() -> {
            try {
                logger.info("🔄 Tentative de connexion à Deepgram...");
                deepgramClient.connectBlocking();
            } catch (Exception e) {
                logger.error("❌ Erreur connexion Deepgram: {}", e.getMessage(), e);
                deepgramClients.remove(session.getId());
                if (session.isOpen()) {
                    try {
                        String errorMsg = e.getMessage();
                        if (errorMsg == null || errorMsg.isEmpty()) {
                            errorMsg = "Erreur inconnue lors de la connexion à Deepgram";
                        }
                        session.sendMessage(new TextMessage("ERROR: " + errorMsg));
                    } catch (Exception sendError) {
                        logger.error("❌ Erreur envoi message d'erreur: {}", sendError.getMessage());
                    }
                }
            }
        }).start();
    }
    
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        WebSocketClient deepgramClient = deepgramClients.get(session.getId());
        
        if (deepgramClient != null && deepgramClient.isOpen()) {
            ByteBuffer buffer = message.getPayload();
            byte[] audioData = new byte[buffer.remaining()];
            buffer.get(audioData);
            
            // Envoyer les données audio à Deepgram
            deepgramService.sendAudioData(deepgramClient, audioData);
            logger.debug("📤 Données audio envoyées à Deepgram: {} bytes", audioData.length);
        } else {
            logger.warn("⚠️ Client Deepgram non disponible pour session: {}", session.getId());
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        logger.debug("📨 Message texte reçu: {}", payload);
        
        // Gérer les messages de contrôle
        if ("CLOSE".equals(payload)) {
            logger.info("🔌 Fermeture demandée par le client");
            closeDeepgramConnection(session);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("🔌 Connexion WebSocket fermée: {} - {}", session.getId(), status);
        closeDeepgramConnection(session);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("❌ Erreur transport WebSocket: {}", exception.getMessage());
        closeDeepgramConnection(session);
    }
    
    private void closeDeepgramConnection(WebSocketSession session) {
        WebSocketClient deepgramClient = deepgramClients.remove(session.getId());
        if (deepgramClient != null) {
            deepgramService.closeConnection(deepgramClient);
            logger.info("✅ Connexion Deepgram fermée pour session: {}", session.getId());
        }
    }
}

