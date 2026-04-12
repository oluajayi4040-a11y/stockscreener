package stockscreener.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketAlert;

@Service
public class AlertBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Send alert to all subscribers in real time
    public void sendAlert(PremarketAlert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }
}
