package stockscreener.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class PriceWebSocketHandler extends TextWebSocketHandler {

    // All connected frontend WebSocket sessions
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

    // Original method for backward compatibility
    public void broadcastAlert(String symbol, String type) {
        broadcastAlert(symbol, type, "PRIMARY");
    }

    // ⭐ NEW method with candle type support
    public void broadcastAlert(String symbol, String type, String candleType) {
        String json = String.format(
                "{\"symbol\":\"%s\",\"type\":\"%s\",\"candleType\":\"%s\"}",
                symbol, type, candleType
        );

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(json));
                System.out.println("📤 Sent alert: " + json);
            } catch (Exception e) {
                System.out.println("⚠️ Failed to send alert: " + e.getMessage());
            }
        }
    }
}