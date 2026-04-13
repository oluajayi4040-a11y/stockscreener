package stockscreener.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PriceWebSocketHandler extends TextWebSocketHandler {

    // ⭐ All connected frontend WebSocket sessions
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("🔌 Alerts WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("❌ Alerts WebSocket disconnected: " + session.getId());
    }

    // ⭐ Broadcast a breakout alert to all connected clients
    public void broadcastAlert(String symbol, String type) {
        String json = String.format(
                "{\"symbol\":\"%s\",\"type\":\"%s\"}",
                symbol, type
        );

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.out.println("⚠️ Failed to send alert: " + e.getMessage());
            }
        }
    }
}
