package stockscreener.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // Cache for premarket levels
    private final ConcurrentHashMap<String, PremarketLevels> levelsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long PREMARKET_CACHE_TTL_MS = 60000; // 1 minute during premarket
    private static final long MARKET_HOURS_CACHE_TTL_MS = 86400000; // 24 hours (freeze after market open)

    // Cache for company names
    private final ConcurrentHashMap<String, String> companyNameCache = new ConcurrentHashMap<>();

    private final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Autowired
    private PremarketLevelsService premarketLevelsService;

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
     * Get premarket high/low for a symbol using the bars endpoint
     * This fetches only premarket hours data (4:00 AM - 9:30 AM ET)
     */
    public PremarketLevels getPremarketLevels(String symbol) {
        Long timestamp = cacheTimestamps.get(symbol);
        
        // If cache exists
        if (timestamp != null) {
            long age = System.currentTimeMillis() - timestamp;
            
            // After market open, cache for 24 hours (never fetch again until next day)
            if (!isPremarketHours() && !isMarketOpen()) {
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
                    return cached;
                }
            }
            
            // During premarket hours (4:00 AM - 9:30 AM) - refresh every minute
            if (isPremarketHours() && age < PREMARKET_CACHE_TTL_MS) {
                PremarketLevels cached = levelsCache.get(symbol);
                if (cached != null) {
                    return cached;
                }
            }
        }

        // Fetch premarket data from Alpaca API using bars endpoint
        try {
            LocalDate today = LocalDate.now(NEW_YORK);
            
            // ⭐ FIXED: Use RFC3339 timestamps with time (4:00 AM to 9:30 AM ET)
            ZonedDateTime premarketStart = ZonedDateTime.of(today, LocalTime.of(4, 0), NEW_YORK);
            ZonedDateTime premarketEnd = ZonedDateTime.of(today, LocalTime.of(9, 30), NEW_YORK);
            
            // Convert to RFC3339 format (e.g., "2026-04-14T04:00:00-04:00")
            String startTime = premarketStart.toOffsetDateTime().toString();
            String endTime = premarketEnd.toOffsetDateTime().toString();
            
            // Use the bars endpoint to get only premarket data
            String url = String.format(
                "%s/v2/stocks/%s/bars?timeframe=1Min&start=%s&end=%s&limit=1000&feed=sip",
                baseUrl, symbol, startTime, endTime
            );
            
            System.out.println("📡 Fetching premarket data for " + symbol + " from " + startTime + " to " + endTime);
            
            HttpEntity<String> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode bars = root.get("bars");
                
                if (bars != null && bars.isArray() && bars.size() > 0) {
                    double high = 0;
                    double low = Double.MAX_VALUE;
                    double open = 0;
                    boolean firstBar = true;

                    for (JsonNode bar : bars) {
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
                    
                    if (low != Double.MAX_VALUE && high > 0) {
                        PremarketLevels levels = new PremarketLevels(high, low, open);
                        levelsCache.put(symbol, levels);
                        cacheTimestamps.put(symbol, System.currentTimeMillis());
                        premarketLevelsService.setLevels(symbol, levels);
                        
                        System.out.println("✅ Premarket for " + symbol + " - H: " + high + ", L: " + low + ", Open: " + open);
                        return levels;
                    } else {
                        System.out.println("⚠️ No premarket bars found for " + symbol);
                    }
                } else {
                    System.out.println("⚠️ No bars data for " + symbol);
                }
            } else {
                System.out.println("⚠️ Failed to fetch premarket data for " + symbol + " - Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Error fetching premarket for " + symbol + ": " + e.getMessage());
        }
        
        // Return cached value if available, otherwise null
        PremarketLevels cached = levelsCache.get(symbol);
        if (cached != null) {
            System.out.println("📦 Using cached premarket levels for " + symbol);
            return cached;
        }
        
        return null;
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
     * Get company name for a symbol
     * Returns the symbol as fallback (can be enhanced later)
     */
    public String getCompanyName(String symbol) {
        // Check cache first
        if (companyNameCache.containsKey(symbol)) {
            return companyNameCache.get(symbol);
        }
        
        // For now, return the symbol as the company name
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