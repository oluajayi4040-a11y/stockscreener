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

    // Send alert to all STOMP subscribers in real time
    public void sendAlert(PremarketAlert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    // Send breakout alert to raw WebSocket clients (ws://localhost:8080/alerts)
    // Original method for backward compatibility
    public void sendBreakoutAlert(String symbol, String type) {
        sendBreakoutAlert(symbol, type, "PRIMARY");
    }

    // ⭐ NEW METHOD with candle type support
    public void sendBreakoutAlert(String symbol, String type, String candleType) {
        System.out.println("🚨 Broadcasting breakout alert: " + symbol + " (" + type + ") - " + candleType + " candle");
        priceWebSocketHandler.broadcastAlert(symbol, type, candleType);
    }
}