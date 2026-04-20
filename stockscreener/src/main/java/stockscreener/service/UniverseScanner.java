package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.ScanCriteria;

import java.util.ArrayList;
import java.util.List;

@Service
public class UniverseScanner {

    private final MarketDataClient marketData;
    private final DynamicSP500Loader sp500Loader;

    public UniverseScanner(MarketDataClient marketData, DynamicSP500Loader sp500Loader) {
        this.marketData = marketData;
        this.sp500Loader = sp500Loader;
    }

    /**
     * Build the universe of symbols to scan.
     * This version uses the dynamic S&P 500 list from Wikipedia.
     */
    public List<String> buildUniverse(ScanCriteria criteria) {

        List<String> universe = new ArrayList<>();

        // 1. Load S&P 500 symbols dynamically
        List<String> symbols = sp500Loader.getSymbols();

        for (String symbol : symbols) {

            // 2. Apply average volume filter
            if (criteria.getMinAvgVolume() != null) {
                Double avgVol = marketData.getAverageVolume(symbol);
                if (avgVol == null || avgVol < criteria.getMinAvgVolume()) {
                    continue;
                }
            }

            // 3. Apply price filter
            if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
                Double last = marketData.getLastPrice(symbol);

                if (last == null) continue;

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
