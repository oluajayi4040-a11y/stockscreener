package stockscreener.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import stockscreener.repository.WatchlistRepository;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlpacaWebSocketClient implements WebSocket.Listener {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.api.secret}")
    private String apiSecret;

    @Autowired
    private WatchlistRepository watchlistRepository;

    // ⭐ NEW: Inject our WebSocket broadcaster
    @Autowired
    private PriceWebSocketHandler priceWebSocketHandler;

    private WebSocket webSocket;

    // ⭐ Store latest prices here (symbol → price)
    public static final Map<String, Double> latestPrices = new ConcurrentHashMap<>();

    @PostConstruct
    public void connect() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(SSLContext.getDefault())
                    .build();

            webSocket = client.newWebSocketBuilder()
                    .buildAsync(
                            URI.create("wss://stream.data.alpaca.markets/v2/iex"),
                            this
                    )
                    .join();

            // ⭐ Authenticate
            String authMsg = String.format(
                    "{\"action\": \"auth\", \"key\": \"%s\", \"secret\": \"%s\"}",
                    apiKey, apiSecret
            );
            webSocket.sendText(authMsg, true);

            System.out.println("🌐 Alpaca WebSocket connected.");

        } catch (Exception e) {
            System.out.println("❌ WebSocket connection failed: " + e.getMessage());
        }
    }

    // ⭐ Subscribe dynamically to your REAL watchlist
    private void subscribeToWatchlist() {
        List<String> symbols = watchlistRepository.findAllSymbols();

        if (symbols.isEmpty()) {
            System.out.println("⚠️ No symbols in watchlist — nothing to subscribe to.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"action\": \"subscribe\", \"quotes\": [");

        for (int i = 0; i < symbols.size(); i++) {
            sb.append("\"").append(symbols.get(i)).append("\"");
            if (i < symbols.size() - 1) sb.append(",");
        }

        sb.append("]}");

        String subscribeMsg = sb.toString();
        webSocket.sendText(subscribeMsg, true);

        System.out.println("📡 Subscribed to watchlist: " + symbols);
    }

    // ⭐ Public method — call this whenever watchlist changes
    public void refreshSubscriptions() {
        if (webSocket != null) {
            subscribeToWatchlist();
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.println("WebSocket opened.");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        String json = data.toString();

        // ⭐ When authenticated, subscribe dynamically
        if (json.contains("\"authenticated\"")) {
            subscribeToWatchlist();
        }

        // ⭐ Handle quote messages
        if (json.contains("\"T\":\"q\"")) {
            try {
                String symbol = json.split("\"S\":\"")[1].split("\"")[0];

                double bid = Double.parseDouble(json.split("\"bp\":")[1].split(",")[0]);
                double ask = Double.parseDouble(json.split("\"ap\":")[1].split("[,}]")[0]);

                double mid = (bid + ask) / 2;

                latestPrices.put(symbol, mid);

                System.out.println("💹 " + symbol + " = " + mid);

                // ⭐ NEW: Broadcast to frontend WebSocket clients
                priceWebSocketHandler.broadcast(symbol, mid);

            } catch (Exception ignored) {}
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}
