package stockscreener.service;

import stockscreener.dto.QuoteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.*;
import java.util.Map;
import java.util.List;

@Service
public class AlpacaService {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.api.secret}")
    private String apiSecret;

    @Value("${alpaca.api.base}")
    private String alpacaBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ORIGINAL METHOD
    public String getLatestQuote(String symbol) {
        String url = alpacaBaseUrl + "/stocks/" + symbol + "/quotes/latest";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    // CLEAN DTO METHOD
    @SuppressWarnings("unchecked")
    public QuoteDTO getCleanQuote(String symbol) {

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String url = alpacaBaseUrl + "/stocks/" + symbol + "/quotes/latest";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        Map quote = (Map) body.get("quote");

        double askPrice = quote.get("ap") != null ? ((Number) quote.get("ap")).doubleValue() : 0.0;
        double bidPrice = quote.get("bp") != null ? ((Number) quote.get("bp")).doubleValue() : 0.0;

        double price = (askPrice + bidPrice) / 2;

        double prevClose = getPreviousClose(symbol);

        double changePercent = 0.0;
        if (prevClose > 0) {
            changePercent = ((price - prevClose) / prevClose) * 100;
        }

        return new QuoteDTO(symbol, price, changePercent);
    }

    // PREVIOUS CLOSE
    @SuppressWarnings("unchecked")
    public double getPreviousClose(String symbol) {
        String url = alpacaBaseUrl + "/stocks/" + symbol + "/bars?timeframe=1Day&limit=2";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        List<Map> bars = (List<Map>) body.get("bars");

        if (bars == null || bars.isEmpty()) return 0.0;

        Map yesterday = bars.get(bars.size() - 1);
        return yesterday.get("c") != null ? ((Number) yesterday.get("c")).doubleValue() : 0.0;
    }

    // MARKET OPEN
    @SuppressWarnings("unchecked")
    public double getMarketOpen(String symbol) {
        String url = alpacaBaseUrl + "/stocks/" + symbol + "/bars?timeframe=1Day&limit=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        List<Map> bars = (List<Map>) body.get("bars");

        if (bars == null || bars.isEmpty()) return 0.0;

        Map today = bars.get(0);
        return today.get("o") != null ? ((Number) today.get("o")).doubleValue() : 0.0;
    }

    // LATEST PRICE (WebSocket first)
    @SuppressWarnings("unchecked")
    public double getLatestPrice(String symbol) {

        Double wsPrice = AlpacaWebSocketClient.latestPrices.get(symbol);
        if (wsPrice != null && wsPrice > 0) return wsPrice;

        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String url = alpacaBaseUrl + "/stocks/" + symbol + "/quotes/latest";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        Map quote = (Map) body.get("quote");

        if (quote == null) return -1;

        double ask = quote.get("ap") != null ? ((Number) quote.get("ap")).doubleValue() : 0.0;
        double bid = quote.get("bp") != null ? ((Number) quote.get("bp")).doubleValue() : 0.0;

        if (ask == 0 && bid == 0) return -1;

        return (ask + bid) / 2;
    }

    // ⭐ FIXED PREMARKET LEVELS (UTC → Eastern Time)
    @SuppressWarnings("unchecked")
    public PremarketLevels getPremarketLevels(String symbol) {

        String url = alpacaBaseUrl + "/stocks/" + symbol + "/bars?timeframe=1Min&limit=500";

        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map body = response.getBody();
        List<Map> bars = (List<Map>) body.get("bars");

        if (bars == null || bars.isEmpty()) return null;

        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;

        ZoneId eastern = ZoneId.of("America/New_York");

        for (Map bar : bars) {

            String ts = (String) bar.get("t");
            if (ts == null) continue;

            // Parse timestamp as UTC
            Instant instant = Instant.parse(ts);

            // Convert to Eastern Time
            ZonedDateTime etTime = instant.atZone(eastern);
            LocalTime localTime = etTime.toLocalTime();

            // Premarket window: 4:00 AM – 9:30 AM ET
            if (!localTime.isBefore(LocalTime.of(4, 0)) &&
                !localTime.isAfter(LocalTime.of(9, 30))) {

                double h = ((Number) bar.get("h")).doubleValue();
                double l = ((Number) bar.get("l")).doubleValue();

                high = Math.max(high, h);
                low = Math.min(low, l);
            }
        }

        if (high == Double.MIN_VALUE || low == Double.MAX_VALUE) return null;

        return new PremarketLevels(high, low);
    }
}
