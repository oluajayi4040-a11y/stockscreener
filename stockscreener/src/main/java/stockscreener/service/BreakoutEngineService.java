package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.PremarketLevels;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BreakoutEngineService {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    // Breakout window: 9:30–9:40 AM ET
    private static final LocalTime BREAKOUT_START_ET = LocalTime.of(9, 30);
    private static final LocalTime BREAKOUT_END_ET   = LocalTime.of(9, 40);

    // Dashboard visibility cutoff: 3:00 PM CST (4:00 PM ET)
    private static final LocalTime DASHBOARD_CUTOFF_ET = LocalTime.of(16, 0);

    // In-memory per-symbol current candle
    private final Map<String, Candle> currentCandleBySymbol = new ConcurrentHashMap<>();

    // Symbols that have already broken out (persist until cutoff)
    private final Map<String, BreakoutInfo> breakoutBySymbol = new ConcurrentHashMap<>();

    // ⭐ NEW — alert broadcaster
    private final AlertBroadcastService alertBroadcastService;

    public BreakoutEngineService(AlertBroadcastService alertBroadcastService) {
        this.alertBroadcastService = alertBroadcastService;
    }

    public enum Direction {
        BULLISH,
        BEARISH
    }

    public static class BreakoutInfo {
        private final String symbol;
        private final Direction direction;
        private final ZonedDateTime breakoutTimeEt;
        private final double premarketHigh;
        private final double premarketLow;

        public BreakoutInfo(String symbol,
                            Direction direction,
                            ZonedDateTime breakoutTimeEt,
                            double premarketHigh,
                            double premarketLow) {
            this.symbol = symbol;
            this.direction = direction;
            this.breakoutTimeEt = breakoutTimeEt;
            this.premarketHigh = premarketHigh;
            this.premarketLow = premarketLow;
        }

        public String getSymbol() {
            return symbol;
        }

        public Direction getDirection() {
            return direction;
        }

        public ZonedDateTime getBreakoutTimeEt() {
            return breakoutTimeEt;
        }

        public double getPremarketHigh() {
            return premarketHigh;
        }

        public double getPremarketLow() {
            return premarketLow;
        }
    }

    private static class Candle {
        private final ZonedDateTime startEt;
        private double open;
        private double high;
        private double low;
        private double close;

        private Candle(ZonedDateTime startEt, double price) {
            this.startEt = startEt;
            this.open = price;
            this.high = price;
            this.low = price;
            this.close = price;
        }

        private void update(double price) {
            if (price > high) high = price;
            if (price < low)  low = price;
            close = price;
        }

        public ZonedDateTime getStartEt() {
            return startEt;
        }

        public double getOpen() {
            return open;
        }

        public double getHigh() {
            return high;
        }

        public double getLow() {
            return low;
        }

        public double getClose() {
            return close;
        }
    }

    /**
     * Call this from your Finnhub WebSocket tick handler.
     *
     * @param symbol   ticker symbol
     * @param price    last traded price
     * @param tsUtc    tick timestamp in UTC (Instant from feed)
     * @param levels   premarket levels for this symbol (high/low)
     */
    public void onTick(String symbol, double price, Instant tsUtc, PremarketLevels levels) {
        if (levels == null) {
            return; // no premarket levels → no breakout logic
        }

        ZonedDateTime tickTimeEt = tsUtc.atZone(NEW_YORK);
        LocalTime timeEt = tickTimeEt.toLocalTime();

        // Always ignore ticks after dashboard cutoff (we don't care anymore)
        if (timeEt.isAfter(DASHBOARD_CUTOFF_ET)) {
            return;
        }

        // If symbol already broke out, we still update price elsewhere,
        // but no need to process more candles for breakout logic.
        if (breakoutBySymbol.containsKey(symbol)) {
            return;
        }

        // Only build candles and check breakouts inside 9:30–9:40 ET
        if (timeEt.isBefore(BREAKOUT_START_ET) || timeEt.isAfter(BREAKOUT_END_ET)) {
            return;
        }

        // Determine the candle start time (truncate to minute)
        ZonedDateTime candleStartEt = tickTimeEt
                .withSecond(0)
                .withNano(0);

        Candle current = currentCandleBySymbol.get(symbol);

        // New candle started
        if (current == null || !current.getStartEt().equals(candleStartEt)) {
            // Finalize previous candle (if any) and check for breakout
            if (current != null) {
                checkBreakout(symbol, current, levels);
            }
            // Start new candle
            Candle newCandle = new Candle(candleStartEt, price);
            currentCandleBySymbol.put(symbol, newCandle);
        } else {
            // Update existing candle
            current.update(price);
        }
    }

    private void checkBreakout(String symbol, Candle candle, PremarketLevels levels) {
        // If already recorded, skip
        if (breakoutBySymbol.containsKey(symbol)) {
            return;
        }

        double close = candle.getClose();
        double preHigh = levels.getHigh();
        double preLow  = levels.getLow();

        Direction direction = null;

        if (close > preHigh) {
            direction = Direction.BULLISH;
        } else if (close < preLow) {
            direction = Direction.BEARISH;
        }

        if (direction != null) {
            BreakoutInfo info = new BreakoutInfo(
                    symbol,
                    direction,
                    candle.getStartEt().plusMinutes(1), // candle close time
                    preHigh,
                    preLow
            );
            breakoutBySymbol.put(symbol, info);

            // ⭐ NEW — send breakout alert to frontend WebSocket
            if (direction == Direction.BULLISH) {
                alertBroadcastService.sendBreakoutAlert(symbol, "HIGH");
            } else {
                alertBroadcastService.sendBreakoutAlert(symbol, "LOW");
            }
        }
    }

    /**
     * Returns all breakouts that should still be visible on the dashboard.
     * Call this from your WatchlistService / controller.
     */
    public List<BreakoutInfo> getActiveBreakoutsForDashboard() {
        ZonedDateTime nowEt = ZonedDateTime.now(NEW_YORK);
        LocalTime timeEt = nowEt.toLocalTime();

        // After 4:00 PM ET (3 PM CST), hide all breakouts
        if (timeEt.isAfter(DASHBOARD_CUTOFF_ET)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(breakoutBySymbol.values());
    }

    /**
     * Optional: clear state at end of day (e.g., via a scheduled job).
     */
    public void resetForNextSession() {
        currentCandleBySymbol.clear();
        breakoutBySymbol.clear();
    }
}
