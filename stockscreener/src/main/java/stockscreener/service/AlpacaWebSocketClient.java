package stockscreener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import stockscreener.model.PremarketLevels;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AlpacaWebSocketClient {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.api.secret}")
    private String apiSecret;

    private WebSocketSession session;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AlpacaPriceService priceService;
    private final BreakoutEngineService breakoutEngineService;
    private final PremarketLevelsService premarketLevelsService;

    // Track subscribed symbols
    private final CopyOnWriteArrayList<String> subscribedSymbols = new CopyOnWriteArrayList<>();

    public AlpacaWebSocketClient(AlpacaPriceService priceService,
                                 BreakoutEngineService breakoutEngineService,
                                 PremarketLevelsService premarketLevelsService) {
        this.priceService = priceService;
        this.breakoutEngineService = breakoutEngineService;
        this.premarketLevelsService = premarketLevelsService;
    }

    // ⭐ DISABLED: WebSocket causes buffer overflow errors
    // The REST poller (AlpacaPricePoller) is working perfectly for price updates
    // @PostConstruct
    public void connect() {
        // WebSocket disabled - using REST poller instead
        System.out.println("⚠️ Alpaca WebSocket is disabled. Using REST poller for price updates.");
        return;
        
        /* Original WebSocket code disabled
        try {
            String url = "wss://stream.data.alpaca.markets/v2/iex";
            System.out.println("🔌 Connecting to Alpaca WebSocket at: " + url);
            StandardWebSocketClient client = new StandardWebSocketClient();

            client.doHandshake(new WebSocketHandler() {

                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    AlpacaWebSocketClient.this.session = session;
                    System.out.println("✅ Alpaca WebSocket connected successfully");
                    sendAuthMessage();
                    for (String symbol : subscribedSymbols) {
                        subscribe(symbol);
                    }
                }

                @Override
                public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                    try {
                        String payload = message.getPayload().toString();
                        JsonNode root = mapper.readTree(payload);
                        
                        if (root.isArray()) {
                            for (JsonNode msg : root) {
                                processSingleMessage(msg);
                            }
                        } else {
                            processSingleMessage(root);
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Error parsing message: " + e.getMessage());
                    }
                }

                private void processSingleMessage(JsonNode msg) {
                    try {
                        if (msg.has("T") && "success".equals(msg.get("T").asText())) {
                            String responseMsg = msg.has("msg") ? msg.get("msg").asText() : "";
                            if ("authenticated".equals(responseMsg)) {
                                System.out.println("✅ Authenticated! Subscribing to symbols...");
                                for (String symbol : subscribedSymbols) {
                                    sendSubscribeMessage(symbol);
                                }
                            }
                            return;
                        }
                        
                        if (msg.has("T") && "error".equals(msg.get("T").asText())) {
                            System.err.println("❌ Alpaca error: " + msg);
                            return;
                        }
                        
                        if (msg.has("T") && "t".equals(msg.get("T").asText())) {
                            String symbol = msg.get("S").asText();
                            double price = msg.get("p").asDouble();
                            processPriceUpdate(symbol, price, System.currentTimeMillis());
                        }
                        
                        if (msg.has("T") && "q".equals(msg.get("T").asText())) {
                            String symbol = msg.get("S").asText();
                            if (msg.has("ap")) {
                                double askPrice = msg.get("ap").asDouble();
                                if (askPrice > 0) {
                                    processPriceUpdate(symbol, askPrice, System.currentTimeMillis());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Error: " + e.getMessage());
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    System.out.println("⚠️ WebSocket error: " + exception.getMessage());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
                    System.out.println("🔌 WebSocket disconnected: " + closeStatus.getReason());
                    try {
                        Thread.sleep(5000);
                        connect();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                @Override
                public boolean supportsPartialMessages() {
                    return false;
                }
            }, url);
        } catch (Exception e) {
            System.out.println("❌ Failed to connect: " + e.getMessage());
        }
        */
    }

    private void sendAuthMessage() {
        try {
            String authMessage = String.format(
                "{\"action\":\"auth\",\"key\":\"%s\",\"secret\":\"%s\"}",
                apiKey, apiSecret
            );
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(authMessage));
                System.out.println("🔐 Auth sent");
            }
        } catch (Exception e) {
            System.out.println("❌ Auth failed: " + e.getMessage());
        }
    }

    private void sendSubscribeMessage(String symbol) {
        try {
            if (session != null && session.isOpen()) {
                String subscribeMessage = String.format(
                    "{\"action\":\"subscribe\",\"trades\":[\"%s\"],\"quotes\":[\"%s\"]}",
                    symbol, symbol
                );
                session.sendMessage(new TextMessage(subscribeMessage));
                System.out.println("📡 Subscribed: " + symbol);
            }
        } catch (Exception e) {
            System.out.println("❌ Subscribe failed for " + symbol + ": " + e.getMessage());
        }
    }

    public void subscribe(String symbol) {
        if (!subscribedSymbols.contains(symbol)) {
            subscribedSymbols.add(symbol);
        }
        // WebSocket is disabled - subscriptions are handled by the REST poller
        System.out.println("📋 Symbol " + symbol + " added to watchlist (poller will fetch prices)");
    }

    private void processPriceUpdate(String symbol, double price, long timestamp) {
        try {
            priceService.updatePrice(symbol, price);
            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
            breakoutEngineService.onTick(symbol, price, Instant.ofEpochMilli(timestamp), levels);
            System.out.println("📊 Alpaca price update: " + symbol + " @ $" + price);
        } catch (Exception e) {
            System.out.println("❌ Error processing price: " + e.getMessage());
        }
    }
}