package stockscreener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import stockscreener.model.PremarketLevels;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlpacaDataService {

    @Value("${alpaca.api.key}")
    private String apiKey;

    @Value("${alpaca.api.secret}")
    private String apiSecret;

    @Value("${alpaca.base.url}")
    private String baseUrl;

    @Value("${finnhub.api.key}")  // ⭐ Inject Finnhub API key
    private String finnhubApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // Cache for premarket levels
    private final ConcurrentHashMap<String, PremarketLevels> levelsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long PREMARKET_CACHE_TTL_MS = 60000; // 1 minute during premarket
    private static final long MARKET_HOURS_CACHE_TTL_MS = 86400000; // 24 hours (freeze after market open)

    // ⭐ Cache for company names (permanent, no expiration)
    private final ConcurrentHashMap<String, String> companyNameCache = new ConcurrentHashMap<>();

    private final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", apiSecret);
        headers.set("Accept", "application/json");
        return headers;
    }

    /**
     * Check if we are in premarket hours (4:00 AM - 9:30 AM ET)
     */
    private boolean isPremarketHours() {
        ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
        LocalTime time = now.toLocalTime();
        LocalTime premarketStart = LocalTime.of(4, 0);
        LocalTime marketOpen = LocalTime.of(9, 30);
        
        // Only Monday-Friday
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        return !time.isBefore(premarketStart) && time.isBefore(marketOpen);
    }

    /**
     * Check if market is currently open
     */
    private boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(NEW_YORK);
        LocalTime time = now.toLocalTime();
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);
        
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        return !time.isBefore(marketOpen) && time.isBefore(marketClose);
    }

    /**
     * Get premarket high/low for a symbol with smart caching
     */
    public PremarketLevels getPremarketLevels(String symbol) {
        Long timestamp = cacheTimestamps.get(symbol);
        
        // If cache exists
        if (timestamp != null) {
            long age = System.currentTimeMillis() - timestamp;
            
            // After market open, cache for 24 hours (never fetch again until next day)
            if (!isPremarketHours() && !isMarketOpen()) {
                // After market closed, use cached value or return null
                PremarketLevels cached = levelsCache.get(symbol);
                if (cached != null) {
                    return cached;
                }
                return null;
            }
            
            // During market hours (9:30 AM - 4:00 PM) - freeze the premarket levels
            if (isMarketOpen()) {
                PremarketLevels cached = levelsCache.get(symbol);
                if (cached != null) {
                    return cached; // Return frozen premarket levels
                }
                // If no cache during market hours, fetch once
            }
            
            // During premarket hours (4:00 AM - 9:30 AM) - refresh every minute
            if (isPremarketHours() && age < PREMARKET_CACHE_TTL_MS) {
                PremarketLevels cached = levelsCache.get(symbol);
                if (cached != null) {
                    return cached;
                }
            }
        }

        // Fetch from Alpaca API
        try {
            String url = baseUrl + "/v2/stocks/" + symbol + "/snapshot";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());

                double high = 0;
                double low = 0;
                double open = 0;

                // Get current minute bar (includes premarket)
                if (root.has("minuteBar") && !root.get("minuteBar").isNull()) {
                    JsonNode minuteBar = root.get("minuteBar");
                    high = minuteBar.has("h") ? minuteBar.get("h").asDouble() : 0;
                    low = minuteBar.has("l") ? minuteBar.get("l").asDouble() : 0;
                    open = minuteBar.has("o") ? minuteBar.get("o").asDouble() : 0;
                }

                // If minuteBar is not available, get from daily bar
                if (high == 0 && root.has("dailyBar") && !root.get("dailyBar").isNull()) {
                    JsonNode dailyBar = root.get("dailyBar");
                    high = dailyBar.has("h") ? dailyBar.get("h").asDouble() : 0;
                    low = dailyBar.has("l") ? dailyBar.get("l").asDouble() : 0;
                    open = dailyBar.has("o") ? dailyBar.get("o").asDouble() : 0;
                }

                if (high > 0 && low > 0) {
                    PremarketLevels levels = new PremarketLevels(high, low, open);
                    levelsCache.put(symbol, levels);
                    cacheTimestamps.put(symbol, System.currentTimeMillis());
                    
                    String timeOfDay = isPremarketHours() ? "PRE-MARKET" : (isMarketOpen() ? "MARKET HOURS" : "AFTER HOURS");
                    System.out.println("✅ Alpaca premarket for " + symbol + " - H: " + high + ", L: " + low + " (" + timeOfDay + ")");
                    return levels;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Alpaca error for " + symbol + ": " + e.getMessage());
        }
        
        // Return cached value if available, otherwise null
        return levelsCache.get(symbol);
    }

    /**
     * Get previous close price for a symbol
     */
    public Double getPreviousClose(String symbol) {
        try {
            String url = baseUrl + "/v2/stocks/" + symbol + "/snapshot";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                if (root.has("prevDailyBar")) {
                    return root.get("prevDailyBar").get("c").asDouble();
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching previous close for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Get current price for a symbol
     */
    public Double getCurrentPrice(String symbol) {
        try {
            String url = baseUrl + "/v2/stocks/" + symbol + "/snapshot";
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                if (root.has("latestTrade") && !root.get("latestTrade").isNull()) {
                    return root.get("latestTrade").get("p").asDouble();
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching current price for " + symbol + ": " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Get company name for a symbol using Finnhub API
     * Fetches once and caches permanently
     */
    public String getCompanyName(String symbol) {
        // Check cache first
        if (companyNameCache.containsKey(symbol)) {
            return companyNameCache.get(symbol);
        }
        
        try {
            // Use Finnhub API to get company profile
            String url = "https://finnhub.io/api/v1/stock/profile2?symbol=" + symbol + "&token=" + finnhubApiKey;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, null, String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                if (root.has("name") && !root.get("name").isNull()) {
                    String companyName = root.get("name").asText();
                    companyNameCache.put(symbol, companyName);
                    System.out.println("📛 Fetched company name for " + symbol + ": " + companyName);
                    return companyName;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch company name for " + symbol + ": " + e.getMessage());
        }
        
        // Fallback: Return the symbol as name
        companyNameCache.put(symbol, symbol);
        return symbol;
    }

    /**
     * Clear cache (useful for testing or end of day)
     */
    public void clearCache() {
        levelsCache.clear();
        cacheTimestamps.clear();
        companyNameCache.clear();
        System.out.println("🧹 Alpaca cache cleared");
    }
}