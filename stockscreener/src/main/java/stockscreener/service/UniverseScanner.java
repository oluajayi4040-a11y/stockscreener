package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.ScanCriteria;

import java.util.ArrayList;
import java.util.List;

@Service
public class UniverseScanner {

    private final MarketDataClient marketData;
    private final WatchlistService watchlistService;

    public UniverseScanner(MarketDataClient marketData, WatchlistService watchlistService) {
        this.marketData = marketData;
        this.watchlistService = watchlistService;
    }

    /**
     * Build the universe of symbols to scan.
     * This version uses the watchlist, but can easily be replaced
     * with S&P500, high‑liquidity lists, or dynamic universe sources.
     */
    public List<String> buildUniverse(ScanCriteria criteria) {

        List<String> universe = new ArrayList<>();

        // 1. Load symbols (from watchlist for now)
        List<String> symbols = watchlistService.getAllSymbols();

        for (String symbol : symbols) {

            // 2. Apply volume filter
            if (criteria.getMinAvgVolume() != null) {
                Double avgVol = marketData.getAverageVolume(symbol);
                if (avgVol == null || avgVol < criteria.getMinAvgVolume()) {
                    continue;
                }
            }

            // 3. Apply price filter
            if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
                double last = marketData.getLastPrice(symbol);

                if (criteria.getMinPrice() != null && last < criteria.getMinPrice()) {
                    continue;
                }
                if (criteria.getMaxPrice() != null && last > criteria.getMaxPrice()) {
                    continue;
                }
            }

            // 4. Apply options volume filter
            if (criteria.getMinOptionsVolume() != null) {
                Double optVol = marketData.getOptionsVolume(symbol);
                if (optVol == null || optVol < criteria.getMinOptionsVolume()) {
                    continue;
                }
            }

            // 5. Add to final universe
            universe.add(symbol);
        }

        return universe;
    }
}
