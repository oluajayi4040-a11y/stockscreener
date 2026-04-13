package stockscreener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import stockscreener.model.PremarketLevels;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class PolygonService {

    @Value("${polygon.api.key}")
    private String apiKey;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final PremarketLevelsService premarketLevelsService;

    public PolygonService(PremarketLevelsService premarketLevelsService) {
        this.premarketLevelsService = premarketLevelsService;
    }

    public PremarketLevels getPremarketLevels(String symbol) {
        try {
            // ⭐ Use yesterday's date — Polygon does NOT return today's premarket until after open
            LocalDate date = LocalDate.now(ZoneId.of("America/New_York")).minusDays(1);

            String url =
                    "https://api.polygon.io/v2/aggs/ticker/" + symbol +
                    "/range/1/minute/" + date + "/" + date +
                    "?adjusted=true&sort=asc&limit=50000&apiKey=" + apiKey;

            String json = rest.getForObject(url, String.class);
            if (json == null) {
                // ⭐ If Polygon fails, return cached levels instead of null
                return premarketLevelsService.getLevels(symbol);
            }

            JsonNode root = mapper.readTree(json);

            // ⭐ Handle Polygon 429 or error response
            if (root.has("status") && "ERROR".equals(root.get("status").asText())) {
                System.out.println("PolygonService error: " + root.get("error").asText());
                return premarketLevelsService.getLevels(symbol); // fallback
            }

            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                return premarketLevelsService.getLevels(symbol); // fallback
            }

            double high = 0.0;
            double low = Double.MAX_VALUE;
            double open = 0.0;
            boolean firstBar = true;

            // ⭐ Premarket window: 4:00 AM → 9:30 AM Eastern
            ZoneId eastern = ZoneId.of("America/New_York");

            for (JsonNode bar : results) {
                long ts = bar.get("t").asLong(); // UTC ms
                var local = java.time.Instant.ofEpochMilli(ts).atZone(eastern);

                int hour = local.getHour();
                int minute = local.getMinute();

                boolean inPremarket =
                        (hour > 4 || (hour == 4 && minute >= 0)) &&
                        (hour < 9 || (hour == 9 && minute < 30));

                if (inPremarket) {
                    double h = bar.get("h").asDouble();
                    double l = bar.get("l").asDouble();
                    double o = bar.get("o").asDouble();

                    if (h > high) high = h;
                    if (l < low) low = l;
                    if (firstBar) {
                        open = o;
                        firstBar = false;
                    }
                }
            }

            if (low == Double.MAX_VALUE) {
                return new PremarketLevels(0, 0, 0);
            }

            PremarketLevels levels = new PremarketLevels(high, low, open);

            // ⭐ Store levels so WebSocket + breakout engine can use them
            premarketLevelsService.setLevels(symbol, levels);

            return levels;

        } catch (Exception e) {
            System.out.println("PolygonService error: " + e.getMessage());
            // ⭐ Fallback to cached levels
            return premarketLevelsService.getLevels(symbol);
        }
    }

    // ⭐ NEW: Get previous close price
    public Double getPreviousClose(String symbol) {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("America/New_York")).minusDays(1);
            
            String url = "https://api.polygon.io/v2/aggs/ticker/" + symbol +
                    "/prev?adjusted=true&apiKey=" + apiKey;

            String json = rest.getForObject(url, String.class);
            if (json == null) {
                return null;
            }

            JsonNode root = mapper.readTree(json);
            
            if (root.has("status") && "ERROR".equals(root.get("status").asText())) {
                System.out.println("Polygon previous close error: " + root.get("error").asText());
                return null;
            }

            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode firstResult = results.get(0);
                return firstResult.get("c").asDouble(); // 'c' is the closing price
            }
        } catch (Exception e) {
            System.out.println("Error fetching previous close for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    // ⭐ NEW: Get market open (today's opening price)
    public Double getMarketOpen(String symbol) {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            
            String url = "https://api.polygon.io/v2/aggs/ticker/" + symbol +
                    "/range/1/minute/" + today + "/" + today +
                    "?adjusted=true&sort=asc&limit=1&apiKey=" + apiKey;

            String json = rest.getForObject(url, String.class);
            if (json == null) {
                return null;
            }

            JsonNode root = mapper.readTree(json);
            
            if (root.has("status") && "ERROR".equals(root.get("status").asText())) {
                System.out.println("Polygon market open error: " + root.get("error").asText());
                return null;
            }

            JsonNode results = root.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode firstBar = results.get(0);
                return firstBar.get("o").asDouble(); // 'o' is the opening price
            }
        } catch (Exception e) {
            System.out.println("Error fetching market open for " + symbol + ": " + e.getMessage());
        }
        return null;
    }
}