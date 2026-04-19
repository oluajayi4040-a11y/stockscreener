package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.MinuteBar;

import java.util.List;

@Service
public class AlpacaMarketDataService {

    private final AlpacaMarketDataClient alpacaClient;

    public AlpacaMarketDataService(AlpacaMarketDataClient alpacaClient) {
        this.alpacaClient = alpacaClient;
    }

    /**
     * Returns the latest minute bar if available.
     * If Alpaca returns no recent bars (weekend, holiday, after-hours),
     * automatically falls back to the most recent snapshot minute bar.
     */
    public MinuteBar getLatestBar(String symbol) {

        // 1. Try to get the latest minute bars (last 5 minutes)
        List<MinuteBar> bars = alpacaClient.getMinuteBars(symbol);

        if (bars != null && !bars.isEmpty()) {
            return bars.get(0); // latest bar
        }

        // 2. Fallback: ALWAYS available snapshot minute bar
        return alpacaClient.getMostRecentSnapshotBar(symbol);
    }
}
