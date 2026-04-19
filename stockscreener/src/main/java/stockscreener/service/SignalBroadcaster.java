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
     * Legacy broadcaster for old QualifiedSignal objects.
     */
    public void broadcast(QualifiedSignal signal) {
        messagingTemplate.convertAndSend("/topic/signals", signal);
    }

    /**
     * NEW — Broadcast institutional BreakoutSignal objects.
     */
    public void broadcast(SignalEngine.BreakoutSignal signal) {
        messagingTemplate.convertAndSend("/topic/signals", signal);
    }
}
