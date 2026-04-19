package stockscreener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import stockscreener.breakout.engine.SignalEngine;
import stockscreener.breakout.engine.SignalEngine.BreakoutSignal;
import stockscreener.model.MinuteBar;
import stockscreener.model.PremarketLevels;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ScannerService
 *
 * Polls market data, retrieves premarket levels,
 * runs the institutional breakout engine,
 * and broadcasts accepted signals.
 */
@Service
public class ScannerService {

    @Autowired
    private MarketDataClient marketDataClient;

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private SignalBroadcaster signalBroadcaster;

    @Autowired
    private SignalEngine signalEngine;

    /**
     * Runs every second during market hours.
     */
    @Scheduled(fixedRate = 1000)
    public void scan() {
        List<String> symbols = watchlistService.getAllSymbols();

        for (String symbol : symbols) {
            try {
                evaluateSymbol(symbol);
            } catch (Exception ex) {
                System.err.println("Error scanning " + symbol + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Evaluates a single symbol using the institutional breakout engine.
     */
    private void evaluateSymbol(String symbol) {

        // 1. Latest 1‑minute bar
        MinuteBar bar = marketDataClient.getLatestMinuteBar(symbol);
        if (bar == null) {
            return;
        }

        // 2. Premarket levels
        PremarketLevels pm = marketDataClient.getPremarketLevels(symbol);
        if (pm == null) {
            return;
        }

        // 3. VWAP
        Double vwap = marketDataClient.getVWAP(symbol);

        // 4. Average 1‑minute volume
        Long avgVolume = marketDataClient.getAverage1MinVolume(symbol);

        // Convert volume fields to Long (SignalEngine requires Long)
        Long volume = (long) bar.getVolume();
        Long pmVolume = (long) pm.getPremarketVolume();

        // 5. Run institutional breakout engine
        BreakoutSignal signal = signalEngine.evaluate(
                symbol,
                bar.getOpen(),
                bar.getClose(),
                bar.getClose(),      // lastPrice
                pm.getHigh(),
                pm.getLow(),
                vwap,
                volume,              // FIXED
                avgVolume,           // FIXED
                pmVolume,            // FIXED
                pm.getPreviousClose(),
                LocalDateTime.now()
        );

        // 6. Broadcast accepted breakouts
        if (signal.isBreakout() && signal.isAccepted()) {
            signalBroadcaster.broadcast(signal);
        }
    }
}
