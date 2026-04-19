package stockscreener.controller;

import org.springframework.web.bind.annotation.*;
import stockscreener.service.MarketDataClient;
import stockscreener.service.AlpacaMarketDataService;
import stockscreener.model.MinuteBar;
import stockscreener.model.PremarketLevels;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final MarketDataClient marketDataClient;
    private final AlpacaMarketDataService marketDataService;

    public MarketDataController(MarketDataClient marketDataClient,
                                AlpacaMarketDataService marketDataService) {
        this.marketDataClient = marketDataClient;
        this.marketDataService = marketDataService;
    }

    // ------------------------------------------------------------
    // NEW: Uses service layer with fallback logic (Option 2)
    // ------------------------------------------------------------
    @GetMapping("/latest-bar/{symbol}")
    public MinuteBar getLatestBar(@PathVariable String symbol) {
        return marketDataService.getLatestBar(symbol);
    }

    // ------------------------------------------------------------
    // Existing endpoints (unchanged)
    // ------------------------------------------------------------
    @GetMapping("/premarket/{symbol}")
    public PremarketLevels getPremarket(@PathVariable String symbol) {
        return marketDataClient.getPremarketLevels(symbol);
    }

    @GetMapping("/vwap/{symbol}")
    public Double getVWAP(@PathVariable String symbol) {
        return marketDataClient.getVWAP(symbol);
    }

    @GetMapping("/avg-volume/{symbol}")
    public Long getAvgVolume(@PathVariable String symbol) {
        return marketDataClient.getAverage1MinVolume(symbol);
    }
}
