package stockscreener.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stockscreener.dto.WatchlistItemDTO;
import stockscreener.model.Watchlist;
import stockscreener.model.PremarketLevels;
import stockscreener.repository.WatchlistRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistRepository repo;

    @Autowired
    private FinnhubWebSocketClient finnhubWebSocketClient;

    @Autowired
    private FinnhubPriceService finnhubPriceService;

    @Autowired
    private BreakoutEngineService breakoutEngineService;

    @Autowired
    private PremarketLevelsService premarketLevelsService;

    @Autowired
    private AlpacaDataService alpacaDataService;

    // Cache for previousClose and marketOpen to reduce API calls
    private final ConcurrentHashMap<String, Double> previousCloseCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> marketOpenCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300000; // 5 minutes
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    // Load premarket levels for ALL symbols at app startup (no delays - Alpaca has no rate limits)
    @PostConstruct
    public void loadPremarketLevelsOnStartup() {
        List<String> symbols = repo.findAllSymbols();
        System.out.println("📋 Loading premarket levels for " + symbols.size() + " symbols from Alpaca");

        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            System.out.println("🔄 (" + (i+1) + "/" + symbols.size() + ") Loading levels for " + symbol);
            
            PremarketLevels levels = alpacaDataService.getPremarketLevels(symbol);
            if (levels != null && levels.getHigh() > 0) {
                premarketLevelsService.setLevels(symbol, levels);
                System.out.println("✅ " + symbol + " - High: " + levels.getHigh() + ", Low: " + levels.getLow());
            } else {
                System.out.println("⚠️ No premarket levels for " + symbol + " from Alpaca");
            }

            finnhubWebSocketClient.subscribe(symbol);
        }

        System.out.println("⭐ Premarket levels loaded for all symbols at startup.");
    }

    // Helper method to get previous close (with caching using Alpaca)
    private double getPreviousClose(String symbol) {
        Long timestamp = cacheTimestamps.get("prev_" + symbol);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            Double cached = previousCloseCache.get(symbol);
            if (cached != null && cached > 0) {
                return cached;
            }
        }
        
        try {
            Double previousClose = alpacaDataService.getPreviousClose(symbol);
            if (previousClose != null && previousClose > 0) {
                previousCloseCache.put(symbol, previousClose);
                cacheTimestamps.put("prev_" + symbol, System.currentTimeMillis());
                return previousClose;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch previous close for " + symbol + ": " + e.getMessage());
        }
        return 0.0;
    }

    // Helper method to get market open (with caching using Alpaca)
    private double getMarketOpen(String symbol) {
        Long timestamp = cacheTimestamps.get("open_" + symbol);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            Double cached = marketOpenCache.get(symbol);
            if (cached != null && cached > 0) {
                return cached;
            }
        }
        
        try {
            PremarketLevels levels = alpacaDataService.getPremarketLevels(symbol);
            if (levels != null && levels.getOpen() > 0) {
                marketOpenCache.put(symbol, levels.getOpen());
                cacheTimestamps.put("open_" + symbol, System.currentTimeMillis());
                return levels.getOpen();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch market open for " + symbol + ": " + e.getMessage());
        }
        return 0.0;
    }

    // Return full watchlist with premarket levels + real-time price + breakout info
    public List<WatchlistItemDTO> getWatchlistWithPrices() {
        List<Watchlist> watchlistEntities = repo.findAll();
        List<WatchlistItemDTO> result = new ArrayList<>();

        List<BreakoutEngineService.BreakoutInfo> breakouts =
                breakoutEngineService.getActiveBreakoutsForDashboard();

        for (Watchlist entity : watchlistEntities) {
            String symbol = entity.getSymbol();
            String companyName = entity.getCompanyName(); // ⭐ Get stored company name
            
            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
            double preHigh = (levels != null) ? levels.getHigh() : 0.0;
            double preLow  = (levels != null) ? levels.getLow()  : 0.0;

            double latestPrice = finnhubPriceService.getLatestPrice(symbol);
            double previousClose = getPreviousClose(symbol);
            double marketOpen = getMarketOpen(symbol);

            BreakoutEngineService.BreakoutInfo breakout = breakouts.stream()
                    .filter(b -> b.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(null);

            boolean hasBreakout = breakout != null;
            String breakoutDirection = hasBreakout ? breakout.getDirection().name() : null;
            String breakoutTime = hasBreakout ? breakout.getBreakoutTimeEt().toString() : null;

            // ⭐ Build DTO with company name
            WatchlistItemDTO dto = new WatchlistItemDTO(
                    symbol,
                    companyName,  // ⭐ NEW: Pass company name
                    latestPrice,
                    preHigh,
                    preLow,
                    previousClose,
                    marketOpen
            );

            dto.setHasBreakout(hasBreakout);
            dto.setBreakoutDirection(breakoutDirection);
            dto.setBreakoutTime(breakoutTime);

            result.add(dto);
        }

        return result;
    }

    // Add symbol + fetch company name + premarket levels + auto-subscribe WebSocket
    @Transactional
    public Watchlist addSymbol(String symbol) {
        String sym = symbol.toUpperCase();
        
        // ⭐ Fetch company name from Alpaca (or fallback to symbol)
        String companyName = alpacaDataService.getCompanyName(sym);
        
        // ⭐ Create watchlist entry with company name
        Watchlist item = new Watchlist(sym, companyName);
        Watchlist saved = repo.save(item);

        System.out.println("📡 Fetching premarket levels for new symbol: " + sym);
        PremarketLevels levels = alpacaDataService.getPremarketLevels(sym);
        if (levels != null && levels.getHigh() > 0) {
            premarketLevelsService.setLevels(sym, levels);
            System.out.println("✅ " + sym + " (" + companyName + ") - High: " + levels.getHigh() + ", Low: " + levels.getLow());
        } else {
            System.out.println("⚠️ No premarket levels for " + sym);
        }

        finnhubWebSocketClient.subscribe(sym);

        return saved;
    }

    // Remove symbol with @Transactional and error handling
    @Transactional
    public void removeSymbol(String symbol) {
        try {
            String sym = symbol.toUpperCase();
            System.out.println("🗑️ Attempting to remove " + sym + " from watchlist");
            
            repo.deleteBySymbol(sym);
            
            // Clean up caches
            previousCloseCache.remove(sym);
            marketOpenCache.remove(sym);
            cacheTimestamps.remove("prev_" + sym);
            cacheTimestamps.remove("open_" + sym);
            
            System.out.println("✅ Removed " + sym + " from watchlist and caches");
        } catch (Exception e) {
            System.err.println("❌ Error removing symbol " + symbol + ": " + e.getMessage());
            throw new RuntimeException("Failed to remove symbol: " + symbol, e);
        }
    }

    // Optional: Clear all caches (useful for testing)
    public void clearCaches() {
        previousCloseCache.clear();
        marketOpenCache.clear();
        cacheTimestamps.clear();
        System.out.println("🧹 All caches cleared");
    }
}