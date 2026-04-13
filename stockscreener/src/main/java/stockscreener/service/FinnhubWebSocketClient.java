package stockscreener.service;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import stockscreener.model.PremarketLevels;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FinnhubWebSocketClient {

    @Value("${finnhub.api.key}")
    private String apiKey;

    private final FinnhubPriceService priceService;
    private final BreakoutEngineService breakoutEngineService;
    private final PremarketLevelsService premarketLevelsService;
    private final SimpMessagingTemplate messagingTemplate;
    
    private static final int MAX_SYMBOLS_PER_CONNECTION = 30;
    
    @Autowired
    private AlpacaDataService alpacaDataService;

    // List of all active WebSocket sessions
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    // Map symbol -> connection index (0, 1, 2...)
    private final ConcurrentHashMap<String, Integer> symbolToConnection = new ConcurrentHashMap<>();
    
    // List of symbols per connection
    private final List<CopyOnWriteArrayList<String>> connectionSymbols = new CopyOnWriteArrayList<>();
    
    // Connection counter
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    public FinnhubWebSocketClient(FinnhubPriceService priceService,
                                  BreakoutEngineService breakoutEngineService,
                                  PremarketLevelsService premarketLevelsService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.priceService = priceService;
        this.breakoutEngineService = breakoutEngineService;
        this.premarketLevelsService = premarketLevelsService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void connect() {
        // Start with 2 connections for up to 60 symbols
        addNewConnection(); // Connection #0
        addNewConnection(); // Connection #1
        System.out.println("🚀 Finnhub WebSocket: 2 connections initialized (supports up to 60 symbols)");
    }

    private void addNewConnection() {
        try {
            String url = "wss://ws.finnhub.io?token=" + apiKey;
            StandardWebSocketClient client = new StandardWebSocketClient();
            int connectionId = connectionCount.getAndIncrement();
            
            connectionSymbols.add(new CopyOnWriteArrayList<>());

            client.doHandshake(new WebSocketHandler() {

                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    sessions.add(session);
                    System.out.println("✅ Finnhub WebSocket connection #" + connectionId + " established");
                    
                    // Re-subscribe to symbols for this connection
                    for (String symbol : connectionSymbols.get(connectionId)) {
                        subscribeOnConnection(session, symbol);
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

                            priceService.updatePrice(symbol, price);

                            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
                            
                            if (levels == null || levels.getHigh() == 0) {
                                levels = alpacaDataService.getPremarketLevels(symbol);
                                if (levels != null && levels.getHigh() > 0) {
                                    premarketLevelsService.setLevels(symbol, levels);
                                }
                            }

                            breakoutEngineService.onTick(
                                    symbol,
                                    price,
                                    Instant.ofEpochMilli(timestampMs),
                                    levels
                            );

                            broadcastPriceUpdate(symbol, price);
                        }

                    } catch (Exception e) {
                        System.out.println("❌ Error handling Finnhub message: " + e.getMessage());
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    System.out.println("⚠️ Finnhub WebSocket error on connection #" + connectionId + ": " + exception.getMessage());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    sessions.remove(session);
                    System.out.println("🔌 Finnhub WebSocket connection #" + connectionId + " closed");
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }

            }, url);

        } catch (Exception e) {
            System.out.println("❌ Failed to create Finnhub WebSocket connection: " + e.getMessage());
        }
    }

    private void subscribeOnConnection(WebSocketSession session, String symbol) {
        try {
            if (session != null && session.isOpen()) {
                String msg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
                session.sendMessage(new TextMessage(msg));
                System.out.println("📡 Subscribed to Finnhub: " + symbol);
            }
        } catch (Exception e) {
            System.out.println("❌ Error subscribing to " + symbol + ": " + e.getMessage());
        }
    }

    public void subscribe(String symbol) {
        // Check if symbol is already subscribed
        if (symbolToConnection.containsKey(symbol)) {
            return;
        }
        
        // Find a connection with available slots
        int connectionId = -1;
        for (int i = 0; i < connectionSymbols.size(); i++) {
            if (connectionSymbols.get(i).size() < MAX_SYMBOLS_PER_CONNECTION) {
                connectionId = i;
                break;
            }
        }
        
        // If no available connection, create a new one
        if (connectionId == -1) {
            addNewConnection();
            connectionId = connectionSymbols.size() - 1;
        }
        
        // Add symbol to the connection
        connectionSymbols.get(connectionId).add(symbol);
        symbolToConnection.put(symbol, connectionId);
        
        // Subscribe on the existing session if available
        if (sessions.size() > connectionId && sessions.get(connectionId) != null) {
            subscribeOnConnection(sessions.get(connectionId), symbol);
        }
        
        System.out.println("📋 Symbol " + symbol + " assigned to connection #" + connectionId + 
                          " (" + connectionSymbols.get(connectionId).size() + "/" + MAX_SYMBOLS_PER_CONNECTION + " symbols)");
    }

    private void broadcastPriceUpdate(String symbol, double price) {
        try {
            JSONObject priceMessage = new JSONObject();
            priceMessage.put("symbol", symbol);
            priceMessage.put("price", price);
            priceMessage.put("timestamp", Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/prices", priceMessage.toString());
        } catch (Exception e) {
            System.out.println("⚠️ Error broadcasting price update: " + e.getMessage());
        }
    }
}