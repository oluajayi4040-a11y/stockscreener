package stockscreener.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stockscreener.dto.WatchlistItemDTO;
import stockscreener.model.Watchlist;
import stockscreener.model.PremarketLevels;
import stockscreener.repository.WatchlistRepository;

import java.util.ArrayList;
import java.util.List;

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
    private PolygonService polygonService;

    // ⭐ Load premarket levels for ALL symbols at app startup
    @PostConstruct
    public void loadPremarketLevelsOnStartup() {
        List<String> symbols = repo.findAllSymbols();

        for (String symbol : symbols) {
            PremarketLevels levels = polygonService.getPremarketLevels(symbol);
            if (levels != null) {
                premarketLevelsService.setLevels(symbol, levels);
            }

            // ⭐ Auto-subscribe WebSocket on startup too
            finnhubWebSocketClient.subscribe(symbol);
        }

        System.out.println("⭐ Premarket levels loaded for all symbols at startup.");
    }

    // ⭐ Helper method to get previous close (from Polygon or other source)
    private double getPreviousClose(String symbol) {
        try {
            // Try to get previous close from Polygon service
            Double previousClose = polygonService.getPreviousClose(symbol);
            if (previousClose != null && previousClose > 0) {
                return previousClose;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch previous close for " + symbol + ": " + e.getMessage());
        }
        // Return 0 if not available (frontend will show "—")
        return 0.0;
    }

    // ⭐ Helper method to get market open (opening price of current day)
    private double getMarketOpen(String symbol) {
        try {
            // First try to get from premarket levels (the open should be the first price of the day)
            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
            if (levels != null && levels.getOpen() > 0) {
                return levels.getOpen();
            }
            
            // Alternative: get from Polygon opening price
            Double marketOpen = polygonService.getMarketOpen(symbol);
            if (marketOpen != null && marketOpen > 0) {
                return marketOpen;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not fetch market open for " + symbol + ": " + e.getMessage());
        }
        // Return 0 if not available (frontend will show "—")
        return 0.0;
    }

    // ⭐ Return full watchlist with premarket levels + real-time price + breakout info
    public List<WatchlistItemDTO> getWatchlistWithPrices() {
        List<String> symbols = repo.findAllSymbols();
        List<WatchlistItemDTO> result = new ArrayList<>();

        // ⭐ Get all active breakouts once (fast)
        List<BreakoutEngineService.BreakoutInfo> breakouts =
                breakoutEngineService.getActiveBreakoutsForDashboard();

        for (String symbol : symbols) {

            // ⭐ Premarket levels now come from in-memory store (NOT Polygon)
            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
            double preHigh = (levels != null) ? levels.getHigh() : 0.0;
            double preLow  = (levels != null) ? levels.getLow()  : 0.0;

            // Real-time price from Finnhub WebSocket
            double latestPrice = finnhubPriceService.getLatestPrice(symbol);

            // ⭐ Get previous close and market open
            double previousClose = getPreviousClose(symbol);
            double marketOpen = getMarketOpen(symbol);

            // ⭐ Check if this symbol broke out
            BreakoutEngineService.BreakoutInfo breakout = breakouts.stream()
                    .filter(b -> b.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(null);

            boolean hasBreakout = breakout != null;
            String breakoutDirection = hasBreakout ? breakout.getDirection().name() : null;
            String breakoutTime = hasBreakout ? breakout.getBreakoutTimeEt().toString() : null;

            // ⭐ Build DTO with all 6 required fields
            WatchlistItemDTO dto = new WatchlistItemDTO(
                    symbol,
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

    // ⭐ Add symbol + fetch premarket levels once + auto-subscribe WebSocket
    public Watchlist addSymbol(String symbol) {
        String sym = symbol.toUpperCase();
        Watchlist item = new Watchlist(sym);
        Watchlist saved = repo.save(item);

        // ⭐ Fetch premarket levels ONCE when symbol is added
        PremarketLevels levels = polygonService.getPremarketLevels(sym);
        if (levels != null) {
            premarketLevelsService.setLevels(sym, levels);
        }

        // ⭐ Subscribe to WebSocket for real-time price
        finnhubWebSocketClient.subscribe(sym);

        return saved;
    }

    // ⭐ Remove symbol
    public void removeSymbol(String symbol) {
        repo.deleteBySymbol(symbol.toUpperCase());
        // Finnhub has no unsubscribe API, safe to ignore
    }
}