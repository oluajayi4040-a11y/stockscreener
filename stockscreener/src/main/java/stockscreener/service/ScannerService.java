package stockscreener.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import stockscreener.breakout.engine.SignalEngine;
import stockscreener.breakout.engine.SignalEngine.BreakoutSignal;
import stockscreener.model.MinuteBar;
import stockscreener.model.PremarketLevels;
import stockscreener.model.ScanCriteria;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScannerService {

    private final MarketDataClient marketDataClient;
    private final UniverseScanner universeScanner;
    private final SignalBroadcaster signalBroadcaster;
    private final SignalEngine signalEngine;

    public ScannerService(
            MarketDataClient marketDataClient,
            UniverseScanner universeScanner,
            SignalBroadcaster signalBroadcaster,
            SignalEngine signalEngine
    ) {
        this.marketDataClient = marketDataClient;
        this.universeScanner = universeScanner;
        this.signalBroadcaster = signalBroadcaster;
        this.signalEngine = signalEngine;
    }

    /**
     * Runs every second during market hours.
     * Scans the dynamically filtered S&P 500 universe.
     */
    @Scheduled(fixedRate = 1000)
    public void scan() {

        // Build universe with your institutional filters
        ScanCriteria criteria = new ScanCriteria();
        criteria.setMinAvgVolume(2_000_000.0);
        criteria.setMinOptionsVolume(10_000.0);
        criteria.setMinPrice(1.0);
        criteria.setMaxPrice(2000.0);

        List<String> universe = universeScanner.buildUniverse(criteria);

        for (String symbol : universe) {
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
        if (bar == null) return;

        // 2. Premarket levels
        PremarketLevels pm = marketDataClient.getPremarketLevels(symbol);
        if (pm == null) return;

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
                volume,
                avgVolume,
                pmVolume,
                pm.getPreviousClose(),
                LocalDateTime.now()
        );

        // 6. Broadcast accepted breakouts
        if (signal.isBreakout() && signal.isAccepted()) {
            signalBroadcaster.broadcast(signal);
        }
    }
}
