package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.MinuteBar;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class AlpacaMarketDataService {

    private final AlpacaMarketDataClient alpacaClient;

    public AlpacaMarketDataService(AlpacaMarketDataClient alpacaClient) {
        this.alpacaClient = alpacaClient;
    }

    /**
     * Returns the latest minute bar for a symbol.
     * Uses getLatestMinuteBar() first.
     * Falls back to the most recent bar from the last 5 minutes.
     */
    public MinuteBar getLatestBar(String symbol) {

        // 1. Primary source — Alpaca latest-minute endpoint
        MinuteBar latest = alpacaClient.getLatestMinuteBar(symbol);
        if (latest != null) {
            return latest;
        }

        // 2. Fallback — fetch last 5 minutes of bars
        ZonedDateTime end = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZonedDateTime start = end.minusMinutes(5);

        List<MinuteBar> bars = alpacaClient.getMinuteBars(symbol, start, end);

        if (bars != null && !bars.isEmpty()) {
            return bars.get(bars.size() - 1); // most recent bar
        }

        return null;
    }
}
