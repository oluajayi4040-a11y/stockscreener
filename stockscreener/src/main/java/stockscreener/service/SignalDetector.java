package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.PremarketLevels;
import stockscreener.model.QualifiedSignal;
import stockscreener.model.ScanCriteria;
import stockscreener.model.MinuteBar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SignalDetector {

    private final MarketDataClient marketDataClient;
    private final VWAPCalculator vwapCalculator;

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    public SignalDetector(MarketDataClient marketDataClient,
                          VWAPCalculator vwapCalculator) {
        this.marketDataClient = marketDataClient;
        this.vwapCalculator = vwapCalculator;
    }

    public List<QualifiedSignal> detectSignals(List<String> symbols, ScanCriteria criteria) {

        List<QualifiedSignal> results = new ArrayList<>();

        for (String symbol : symbols) {
            try {
                QualifiedSignal signal = detectSignalForSymbol(symbol, criteria);
                if (signal != null) {
                    results.add(signal);
                }
            } catch (Exception e) {
                System.out.println("⚠ Error detecting signal for " + symbol + ": " + e.getMessage());
            }
        }

        return results;
    }

    private QualifiedSignal detectSignalForSymbol(String symbol, ScanCriteria criteria) {

        // 1. Premarket levels
        PremarketLevels pre = marketDataClient.getPremarketLevels(symbol);
        if (pre == null) {
            return null;
        }

        double preHigh = pre.getHigh();
        double preLow = pre.getLow();

        // 2. Minute bars (ZonedDateTime required)
        LocalDate today = LocalDate.now(NEW_YORK);

        ZonedDateTime start = today.atStartOfDay(NEW_YORK);
        ZonedDateTime end = ZonedDateTime.now(NEW_YORK);

        List<MinuteBar> bars = marketDataClient.getMinuteBars(symbol, start, end);
        if (bars == null || bars.isEmpty()) {
            return null;
        }

        // 3. VWAP
        double vwap = vwapCalculator.computeVWAP(bars);

        // 4. Last price
        double lastPrice = marketDataClient.getLastPrice(symbol);
        if (lastPrice <= 0) {
            return null;
        }

        // 5. Breakout logic
        boolean breakoutUp = lastPrice > preHigh && lastPrice > vwap;
        boolean breakoutDown = lastPrice < preLow && lastPrice < vwap;

        if (!breakoutUp && !breakoutDown) {
            return null;
        }

        // 6. Build signal
        QualifiedSignal signal = new QualifiedSignal();
        signal.setSymbol(symbol);
        signal.setLastPrice(lastPrice);
        signal.setVwap(vwap);
        signal.setPremarketHigh(preHigh);
        signal.setPremarketLow(preLow);

        long ts = ZonedDateTime.now(NEW_YORK).toInstant().toEpochMilli();
        signal.setTimestamp(ts);

        signal.setDirection(breakoutUp ? "BREAKOUT_UP" : "BREAKOUT_DOWN");

        return signal;
    }
}
