package stockscreener.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketAlert;

@Service
public class AlertBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final PriceWebSocketHandler priceWebSocketHandler;

    public AlertBroadcastService(SimpMessagingTemplate messagingTemplate,
                                 PriceWebSocketHandler priceWebSocketHandler) {
        this.messagingTemplate = messagingTemplate;
        this.priceWebSocketHandler = priceWebSocketHandler;
    }

    // ⭐ EXISTING FUNCTION — UNTOUCHED
    // Send alert to all STOMP subscribers in real time
    public void sendAlert(PremarketAlert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    // ⭐ NEW FUNCTION — ADDED WITHOUT CHANGING ANYTHING ABOVE
    // Send breakout alert to raw WebSocket clients (ws://localhost:8080/alerts)
    public void sendBreakoutAlert(String symbol, String type) {
        System.out.println("🚨 Broadcasting breakout alert: " + symbol + " (" + type + ")");
        priceWebSocketHandler.broadcastAlert(symbol, type);
    }
}
