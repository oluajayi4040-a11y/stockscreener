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
        System.out.println("🔌 Frontend connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("❌ Frontend disconnected: " + session.getId());
    }

    // ⭐ Broadcast a single-symbol price update to all connected clients
    public void broadcast(String symbol, double price) {
        String json = String.format(
                "{\"symbol\":\"%s\",\"price\":%.4f}",
                symbol, price
        );

        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                System.out.println("⚠️ Failed to send message: " + e.getMessage());
            }
        }
    }
}
