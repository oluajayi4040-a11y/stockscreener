package stockscreener.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import stockscreener.model.QualifiedSignal;
import stockscreener.breakout.engine.SignalEngine;

@Service
public class SignalBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public SignalBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast legacy QualifiedSignal objects.
     * (Still supported for backward compatibility.)
     */
    public void broadcast(QualifiedSignal signal) {
        messagingTemplate.convertAndSend("/topic/signals", signal);
    }

    /**
     * Broadcast institutional BreakoutSignal objects.
     */
    public void broadcast(SignalEngine.BreakoutSignal signal) {
        messagingTemplate.convertAndSend("/topic/signals", signal);
    }
}
