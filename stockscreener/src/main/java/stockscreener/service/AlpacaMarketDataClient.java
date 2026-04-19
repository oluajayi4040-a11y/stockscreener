package stockscreener.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import stockscreener.model.MinuteBar;
import stockscreener.model.PremarketLevels;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlpacaMarketDataClient implements MarketDataClient {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.api.secret}")
    private String apiSecret;

    @Value("${alpaca.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ------------------------------------------------------------
    // Build authenticated request
    // ------------------------------------------------------------
    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(null, headers);
    }

    private String barsUrl(String symbol) {
        return baseUrl + "/v2/stocks/" + symbol + "/bars";
    }

    private String snapshotUrl(String symbol) {
        return baseUrl + "/v2/stocks/" + symbol + "/snapshot";
    }

    private String quotesUrl(String symbol) {
        return baseUrl + "/v2/stocks/" + symbol + "/quotes/latest";
    }

    // ------------------------------------------------------------
    // 1. Last Price
    // ------------------------------------------------------------
    @Override
    public double getLastPrice(String symbol) {
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(quotesUrl(symbol), HttpMethod.GET, buildEntity(), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            return root.path("quote").path("ap").asDouble();
        } catch (Exception e) {
            System.out.println("⚠ Error fetching last price for " + symbol + ": " + e.getMessage());
            return -1;
        }
    }

    // ------------------------------------------------------------
    // 2. Previous Close
    // ------------------------------------------------------------
    @Override
    public Double getPreviousClose(String symbol) {
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(snapshotUrl(symbol), HttpMethod.GET, buildEntity(), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            return root.path("prevDailyBar").path("c").asDouble();
        } catch (Exception e) {
            System.out.println("⚠ Error fetching previous close for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------
    // 3. Premarket Levels
    // ------------------------------------------------------------
    @Override
    public PremarketLevels getPremarketLevels(String symbol) {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime preStart = now.withHour(4).withMinute(0).withSecond(0);
            ZonedDateTime preEnd = now.withHour(9).withMinute(30).withSecond(0);

            List<MinuteBar> bars = getMinuteBars(symbol, preStart, preEnd);
            if (bars.isEmpty()) return null;

            double high = bars.stream().mapToDouble(MinuteBar::getHigh).max().orElse(0);
            double low = bars.stream().mapToDouble(MinuteBar::getLow).min().orElse(0);
            double open = bars.get(0).getOpen();
            double volume = bars.stream().mapToDouble(MinuteBar::getVolume).sum();

            PremarketLevels levels = new PremarketLevels();
            levels.setHigh(high);
            levels.setLow(low);
            levels.setOpen(open);
            levels.setPremarketVolume(volume);
            levels.setPreviousClose(getPreviousClose(symbol));

            return levels;

        } catch (Exception e) {
            System.out.println("⚠ Error fetching premarket levels for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------
    // 4. Premarket Volume
    // ------------------------------------------------------------
    @Override
    public double getPremarketVolume(String symbol) {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime preStart = now.withHour(4).withMinute(0).withSecond(0);
            ZonedDateTime preEnd = now.withHour(9).withMinute(30).withSecond(0);

            List<MinuteBar> bars = getMinuteBars(symbol, preStart, preEnd);
            return bars.stream().mapToDouble(MinuteBar::getVolume).sum();

        } catch (Exception e) {
            System.out.println("⚠ Error fetching premarket volume for " + symbol + ": " + e.getMessage());
            return 0;
        }
    }

    // ------------------------------------------------------------
    // 5. Minute Bars (range)
    // ------------------------------------------------------------
    @Override
    public List<MinuteBar> getMinuteBars(String symbol, ZonedDateTime start, ZonedDateTime end) {

        List<MinuteBar> bars = new ArrayList<>();

        try {
            String url = barsUrl(symbol)
                    + "?timeframe=1Min"
                    + "&start=" + start.toInstant()
                    + "&end=" + end.toInstant();

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, buildEntity(), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode barArray = root.path("bars");

            if (!barArray.isArray()) return bars;

            for (JsonNode node : barArray) {
                MinuteBar bar = new MinuteBar();
                bar.setTimestamp(ZonedDateTime.parse(node.get("t").asText()));
                bar.setOpen(node.get("o").asDouble());
                bar.setHigh(node.get("h").asDouble());
                bar.setLow(node.get("l").asDouble());
                bar.setClose(node.get("c").asDouble());
                bar.setVolume(node.get("v").asDouble());
                bars.add(bar);
            }

        } catch (Exception e) {
            System.out.println("⚠ Error fetching minute bars for " + symbol + ": " + e.getMessage());
        }

        return bars;
    }

    // ------------------------------------------------------------
    // 5B. Minute Bars (latest 5 minutes)
    // ------------------------------------------------------------
    public List<MinuteBar> getMinuteBars(String symbol) {
        ZonedDateTime end = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZonedDateTime start = end.minusMinutes(5);
        return getMinuteBars(symbol, start, end);
    }

    // ------------------------------------------------------------
    // 6. VWAP
    // ------------------------------------------------------------
    @Override
    public Double getVWAP(String symbol) {
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(snapshotUrl(symbol), HttpMethod.GET, buildEntity(), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            return root.path("minuteBar").path("vw").asDouble();

        } catch (Exception e) {
            System.out.println("⚠ Error fetching VWAP for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------
    // 7. Latest 1‑minute bar
    // ------------------------------------------------------------
    @Override
    public MinuteBar getLatestMinuteBar(String symbol) {
        ZonedDateTime end = ZonedDateTime.now();
        ZonedDateTime start = end.minusMinutes(2);

        List<MinuteBar> bars = getMinuteBars(symbol, start, end);
        if (bars.isEmpty()) return null;

        return bars.get(bars.size() - 1);
    }

    // ------------------------------------------------------------
    // 8. Average 1‑minute volume
    // ------------------------------------------------------------
    @Override
    public Long getAverage1MinVolume(String symbol) {
        ZonedDateTime end = ZonedDateTime.now();
        ZonedDateTime start = end.minusMinutes(30);

        List<MinuteBar> bars = getMinuteBars(symbol, start, end);
        if (bars.isEmpty()) return 0L;

        return (long) bars.stream()
                .mapToDouble(MinuteBar::getVolume)
                .average()
                .orElse(0);
    }

    // ------------------------------------------------------------
    // 9. Current Volume
    // ------------------------------------------------------------
    @Override
    public double getCurrentVolume(String symbol) {
        ZonedDateTime end = ZonedDateTime.now();
        ZonedDateTime start = end.minusMinutes(2);

        List<MinuteBar> bars = getMinuteBars(symbol, start, end);
        if (bars.isEmpty()) return 0;

        return bars.get(bars.size() - 1).getVolume();
    }

    // ------------------------------------------------------------
    // 10. Average Daily Volume
    // ------------------------------------------------------------
    @Override
    public Double getAverageVolume(String symbol) {
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(snapshotUrl(symbol), HttpMethod.GET, buildEntity(), String.class);

            JsonNode root = mapper.readTree(response.getBody());
            return root.path("dailyBar").path("v").asDouble();

        } catch (Exception e) {
            System.out.println("⚠ Error fetching avg volume for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------
    // 11. Fallback: Snapshot Minute Bar (ALWAYS AVAILABLE)
    // ------------------------------------------------------------
    public MinuteBar getMostRecentSnapshotBar(String symbol) {
        try {
            String url = snapshotUrl(symbol);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("APCA-API-KEY-ID", apiKey)
                    .header("APCA-API-SECRET-KEY", apiSecret)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(response.body());
            JsonNode mb = root.path("minuteBar");

            if (!mb.isMissingNode() && mb.has("t")) {
                MinuteBar bar = new MinuteBar();
                bar.setTimestamp(ZonedDateTime.parse(mb.get("t").asText()));
                bar.setOpen(mb.get("o").asDouble());
                bar.setHigh(mb.get("h").asDouble());
                bar.setLow(mb.get("l").asDouble());
                bar.setClose(mb.get("c").asDouble());
                bar.setVolume(mb.get("v").asDouble());
                return bar;
            }

        } catch (Exception e) {
            System.out.println("⚠ Error fetching snapshot fallback bar for " + symbol + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    public Double getOptionsVolume(String symbol) {
        return 20_000.0;
    }

    @Override
    public String getCompanyName(String symbol) {
        return symbol;
    }
}
