package stockscreener.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import stockscreener.model.MinuteBar;
import stockscreener.model.PremarketLevels;

import java.time.Instant;
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

    private static final String BASE_URL = "https://data.alpaca.markets/v2";
    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JSONObject getJson(String url) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return new JSONObject(response.getBody());
        } catch (Exception ex) {
            System.err.println("Error fetching JSON from Alpaca: " + ex.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------
    // IMPLEMENTATION OF MarketDataClient INTERFACE
    // ---------------------------------------------------------

    @Override
    public Double getLastPrice(String symbol) {
        String url = BASE_URL + "/stocks/" + symbol + "/trades/latest";
        JSONObject json = getJson(url);
        if (json == null || !json.has("trade") || json.isNull("trade")) return null;
        return json.getJSONObject("trade").optDouble("p", Double.NaN);
    }

    @Override
    public Double getPreviousClose(String symbol) {
        String url = BASE_URL + "/stocks/" + symbol + "/bars?timeframe=1Day&limit=2";
        JSONObject json = getJson(url);
        if (json == null || !json.has("bars") || json.isNull("bars")) return null;

        JSONArray bars = json.optJSONArray("bars");
        if (bars == null || bars.length() < 2) return null;

        return bars.getJSONObject(0).optDouble("c", Double.NaN);
    }

    @Override
    public PremarketLevels getPremarketLevels(String symbol) {

        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("America/New_York"))
                .withHour(4).withMinute(0).withSecond(0).withNano(0);

        String url = BASE_URL + "/stocks/" + symbol + "/bars?timeframe=1Min&start=" +
                start.toInstant();

        JSONObject json = getJson(url);
        if (json == null || !json.has("bars") || json.isNull("bars")) return null;

        JSONArray bars = json.optJSONArray("bars");
        if (bars == null || bars.length() == 0) return null;

        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        long volume = 0;

        for (int i = 0; i < bars.length(); i++) {
            JSONObject bar = bars.getJSONObject(i);
            high = Math.max(high, bar.optDouble("h", 0));
            low = Math.min(low, bar.optDouble("l", 0));
            volume += bar.optLong("v", 0);
        }

        Double prevClose = getPreviousClose(symbol);

        return new PremarketLevels(high, low, volume, prevClose);
    }

    @Override
    public Double getPremarketVolume(String symbol) {
        PremarketLevels pm = getPremarketLevels(symbol);
        return pm != null ? (double) pm.getPremarketVolume() : null;
    }

    @Override
    public Double getCurrentVolume(String symbol) {
        String url = BASE_URL + "/stocks/" + symbol + "/bars/latest";
        JSONObject json = getJson(url);
        if (json == null || !json.has("bar") || json.isNull("bar")) return null;
        return json.getJSONObject("bar").optDouble("v", Double.NaN);
    }

    @Override
    public Double getAverageVolume(String symbol) {
        String url = BASE_URL + "/stocks/" + symbol + "/bars?timeframe=1Day&limit=20";
        JSONObject json = getJson(url);

        if (json == null || !json.has("bars") || json.isNull("bars")) {
            return null;
        }

        JSONArray bars = json.optJSONArray("bars");
        if (bars == null || bars.length() == 0) {
            return null;
        }

        long total = 0;
        for (int i = 0; i < bars.length(); i++) {
            total += bars.getJSONObject(i).optLong("v", 0);
        }

        return (double) (total / bars.length());
    }

    @Override
    public Double getOptionsVolume(String symbol) {
        // Placeholder until OPRA integration
        return 20_000.0;
    }

    @Override
    public String getCompanyName(String symbol) {
        return symbol;
    }

    @Override
    public List<MinuteBar> getMinuteBars(String symbol, ZonedDateTime start, ZonedDateTime end) {

        String url = BASE_URL + "/stocks/" + symbol + "/bars?timeframe=1Min&start=" +
                start.toInstant() + "&end=" + end.toInstant();

        JSONObject json = getJson(url);
        List<MinuteBar> bars = new ArrayList<>();

        if (json == null || !json.has("bars") || json.isNull("bars")) return bars;

        JSONArray arr = json.optJSONArray("bars");
        if (arr == null) return bars;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject b = arr.getJSONObject(i);

            bars.add(new MinuteBar(
                    b.optDouble("o", 0),
                    b.optDouble("h", 0),
                    b.optDouble("l", 0),
                    b.optDouble("c", 0),
                    b.optLong("v", 0),
                    ZonedDateTime.ofInstant(Instant.parse(b.getString("t")), ZoneId.of("UTC"))
            ));
        }

        return bars;
    }

    @Override
    public MinuteBar getLatestMinuteBar(String symbol) {
        String url = BASE_URL + "/stocks/" + symbol + "/bars/latest";
        JSONObject json = getJson(url);
        if (json == null || !json.has("bar") || json.isNull("bar")) return null;

        JSONObject b = json.getJSONObject("bar");

        return new MinuteBar(
                b.optDouble("o", 0),
                b.optDouble("h", 0),
                b.optDouble("l", 0),
                b.optDouble("c", 0),
                b.optLong("v", 0),
                ZonedDateTime.ofInstant(Instant.parse(b.getString("t")), ZoneId.of("UTC"))
        );
    }

    @Override
    public Double getVWAP(String symbol) {
        MinuteBar bar = getLatestMinuteBar(symbol);
        if (bar == null) return null;

        return (bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0;
    }

    @Override
    public Long getAverage1MinVolume(String symbol) {

        ZonedDateTime end = ZonedDateTime.now();
        ZonedDateTime start = end.minusMinutes(20);

        List<MinuteBar> bars = getMinuteBars(symbol, start, end);

        if (bars.isEmpty()) return null;

        long total = 0;
        for (MinuteBar bar : bars) {
            total += bar.getVolume();
        }

        return total / bars.size();
    }
}
