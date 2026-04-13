package stockscreener.service;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FinnhubWebSocketClient {

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final FinnhubPriceService priceService;
    private final BreakoutEngineService breakoutEngineService;
    private final PremarketLevelsService premarketLevelsService;
    private final SimpMessagingTemplate messagingTemplate;  // ⭐ NEW: For STOMP broadcasts

    private WebSocketSession session;

    private final CopyOnWriteArrayList<String> subscribedSymbols = new CopyOnWriteArrayList<>();

    public FinnhubWebSocketClient(FinnhubPriceService priceService,
                                  BreakoutEngineService breakoutEngineService,
                                  PremarketLevelsService premarketLevelsService,
                                  SimpMessagingTemplate messagingTemplate) {  // ⭐ NEW constructor param
        this.priceService = priceService;
        this.breakoutEngineService = breakoutEngineService;
        this.premarketLevelsService = premarketLevelsService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void connect() {
        try {
            String url = "wss://ws.finnhub.io?token=" + apiKey;
            StandardWebSocketClient client = new StandardWebSocketClient();

            client.doHandshake(new WebSocketHandler() {

                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    FinnhubWebSocketClient.this.session = session;

                    // Re-subscribe to all symbols after reconnect
                    for (String symbol : subscribedSymbols) {
                        subscribe(symbol);
                    }
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    try {
                        String payload = message.getPayload().toString();
                        JSONObject json = new JSONObject(payload);

                        if (!json.has("data")) return;

                        JSONArray dataArray = json.getJSONArray("data");

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject obj = dataArray.getJSONObject(i);

                            String symbol = obj.getString("s");
                            double price = obj.getDouble("p");
                            long timestampMs = obj.getLong("t");

                            // ⭐ Update real-time price store
                            priceService.updatePrice(symbol, price);

                            // ⭐ Feed tick into breakout engine
                            breakoutEngineService.onTick(
                                    symbol,
                                    price,
                                    Instant.ofEpochMilli(timestampMs),
                                    premarketLevelsService.getLevels(symbol)
                            );

                            // ⭐ NEW: Broadcast price update to all STOMP clients
                            broadcastPriceUpdate(symbol, price);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    exception.printStackTrace();
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    System.out.println("Finnhub WebSocket closed: " + closeStatus.getReason());
                    // Optional: auto-reconnect logic can be added here
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }

            }, url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ⭐ NEW: Broadcast price update to STOMP clients
    private void broadcastPriceUpdate(String symbol, double price) {
        try {
            JSONObject priceMessage = new JSONObject();
            priceMessage.put("symbol", symbol);
            priceMessage.put("price", price);
            priceMessage.put("timestamp", Instant.now().toString());

            // Send to /topic/prices (your frontend is subscribed here)
            messagingTemplate.convertAndSend("/topic/prices", priceMessage.toString());
        } catch (Exception e) {
            System.out.println("Error broadcasting price update: " + e.getMessage());
        }
    }

    public void subscribe(String symbol) {
        try {
            if (session != null && session.isOpen()) {
                String msg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
                session.sendMessage(new TextMessage(msg));
            }

            if (!subscribedSymbols.contains(symbol)) {
                subscribedSymbols.add(symbol);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}