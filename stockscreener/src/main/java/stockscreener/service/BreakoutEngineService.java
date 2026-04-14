package stockscreener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketLevels;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BreakoutEngineService {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    // Primary breakout window: 8:30 - 8:40 AM CST (1-min and 5-min candles)
    private static final LocalTime PRIMARY_START_CST = LocalTime.of(8, 30);
    private static final LocalTime PRIMARY_END_CST = LocalTime.of(8, 40);
    
    // Secondary breakout window: 8:45 - 9:02 AM CST (15-min candles only)
    // This captures the 9:00 AM 15-minute candle close
    private static final LocalTime SECONDARY_START_CST = LocalTime.of(8, 45);
    private static final LocalTime SECONDARY_END_CST = LocalTime.of(9, 2);

    // Dashboard visibility cutoff: 10 PM CST (11 PM ET)
    private static final LocalTime DASHBOARD_CUTOFF_ET = LocalTime.of(23, 0);

    private final Map<String, Candle> currentOneMinuteCandleBySymbol = new ConcurrentHashMap<>();
    private final Map<String, FiveMinuteCandle> currentFiveMinuteCandleBySymbol = new ConcurrentHashMap<>();
    private final Map<String, FifteenMinuteCandle> currentFifteenMinuteCandleBySymbol = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> triggeredBreakouts = new ConcurrentHashMap<>();

    private final AlertBroadcastService alertBroadcastService;
    
    @Autowired
    private PremarketLevelsService premarketLevelsService;
    
    @Autowired
    private AlpacaDataService alpacaDataService;

    public BreakoutEngineService(AlertBroadcastService alertBroadcastService) {
        this.alertBroadcastService = alertBroadcastService;
    }

    public enum Direction { BULLISH, BEARISH }
    public enum CandleType { ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE }

    public static class BreakoutInfo {
        private final String symbol;
        private final Direction direction;
        private final ZonedDateTime breakoutTimeEt;
        private final double premarketHigh;
        private final double premarketLow;
        private final CandleType candleType;
        private final double triggeredPrice;

        public BreakoutInfo(String symbol, Direction direction, ZonedDateTime breakoutTimeEt,
                            double premarketHigh, double premarketLow, CandleType candleType, double triggeredPrice) {
            this.symbol = symbol;
            this.direction = direction;
            this.breakoutTimeEt = breakoutTimeEt;
            this.premarketHigh = premarketHigh;
            this.premarketLow = premarketLow;
            this.candleType = candleType;
            this.triggeredPrice = triggeredPrice;
        }

        public String getSymbol() { return symbol; }
        public Direction getDirection() { return direction; }
        public ZonedDateTime getBreakoutTimeEt() { return breakoutTimeEt; }
        public double getPremarketHigh() { return premarketHigh; }
        public double getPremarketLow() { return premarketLow; }
        public CandleType getCandleType() { return candleType; }
        public double getTriggeredPrice() { return triggeredPrice; }
        public String getAlertType() { return direction == Direction.BULLISH ? "HIGH" : "LOW"; }
    }

    private static class Candle {
        private final ZonedDateTime startEt;
        private double open, high, low, close;
        private Candle(ZonedDateTime startEt, double price) {
            this.startEt = startEt;
            this.open = this.high = this.low = this.close = price;
        }
        private void update(double price) {
            if (price > high) high = price;
            if (price < low) low = price;
            close = price;
        }
        public ZonedDateTime getStartEt() { return startEt; }
        public double getClose() { return close; }
    }

    private static class FiveMinuteCandle {
        private final ZonedDateTime startEt;
        private double open, high, low, close;
        private FiveMinuteCandle(ZonedDateTime startEt, double price) {
            this.startEt = startEt;
            this.open = this.high = this.low = this.close = price;
        }
        private void update(double price) {
            if (price > high) high = price;
            if (price < low) low = price;
            close = price;
        }
        public ZonedDateTime getStartEt() { return startEt; }
        public double getClose() { return close; }
    }

    private static class FifteenMinuteCandle {
        private final ZonedDateTime startEt;
        private double open, high, low, close;
        private FifteenMinuteCandle(ZonedDateTime startEt, double price) {
            this.startEt = startEt;
            this.open = this.high = this.low = this.close = price;
        }
        private void update(double price) {
            if (price > high) high = price;
            if (price < low) low = price;
            close = price;
        }
        public ZonedDateTime getStartEt() { return startEt; }
        public double getClose() { return close; }
    }

    private PremarketLevels getArtificialLevels(String symbol) {
        try {
            Double previousClose = alpacaDataService.getPreviousClose(symbol);
            if (previousClose != null && previousClose > 0) {
                System.out.println("⚠️ Using ARTIFICIAL levels for " + symbol + " (previous close: $" + previousClose + ")");
                return new PremarketLevels(previousClose * 1.02, previousClose * 0.98, previousClose);
            }
        } catch (Exception e) { }
        return null;
    }

    private String getTriggerKey(String symbol, Direction direction, CandleType candleType) {
        return symbol + "_" + direction + "_" + candleType;
    }

    private boolean hasTriggered(String symbol, Direction direction, CandleType candleType) {
        Set<String> triggers = triggeredBreakouts.get(symbol);
        return triggers != null && triggers.contains(getTriggerKey(symbol, direction, candleType));
    }

    private void markTriggered(String symbol, Direction direction, CandleType candleType) {
        triggeredBreakouts.computeIfAbsent(symbol, k -> ConcurrentHashMap.newKeySet())
                          .add(getTriggerKey(symbol, direction, candleType));
    }

    public void onTick(String symbol, double price, Instant tsUtc, PremarketLevels levels) {
        if (levels == null || levels.getHigh() == 0) {
            levels = getArtificialLevels(symbol);
            if (levels == null) return;
        }

        ZonedDateTime tickTimeCst = tsUtc.atZone(CHICAGO);
        LocalTime timeCst = tickTimeCst.toLocalTime();

        if (ZonedDateTime.now(NEW_YORK).toLocalTime().isAfter(DASHBOARD_CUTOFF_ET)) return;

        // Primary window: 8:30 - 8:40 AM CST (1-min and 5-min candles)
        if (timeCst.isAfter(PRIMARY_START_CST) && timeCst.isBefore(PRIMARY_END_CST)) {
            processOneMinuteCandle(symbol, price, tickTimeCst, levels);
            processFiveMinuteCandle(symbol, price, tickTimeCst, levels);
        }

        // Secondary window: 8:45 - 9:02 AM CST (15-min candles only)
        if (timeCst.isAfter(SECONDARY_START_CST) && timeCst.isBefore(SECONDARY_END_CST)) {
            processFifteenMinuteCandle(symbol, price, tickTimeCst, levels);
        }
    }

    private void processOneMinuteCandle(String symbol, double price, ZonedDateTime tickTimeCst, PremarketLevels levels) {
        ZonedDateTime candleStart = tickTimeCst.withSecond(0).withNano(0);
        Candle current = currentOneMinuteCandleBySymbol.get(symbol);
        if (current == null || !current.getStartEt().equals(candleStart)) {
            if (current != null) checkBreakout(symbol, current.getClose(), levels, CandleType.ONE_MINUTE);
            currentOneMinuteCandleBySymbol.put(symbol, new Candle(candleStart, price));
        } else {
            current.update(price);
        }
    }

    private void processFiveMinuteCandle(String symbol, double price, ZonedDateTime tickTimeCst, PremarketLevels levels) {
        int intervalStart = (tickTimeCst.getMinute() / 5) * 5;
        ZonedDateTime candleStart = tickTimeCst.withMinute(intervalStart).withSecond(0).withNano(0);
        FiveMinuteCandle current = currentFiveMinuteCandleBySymbol.get(symbol);
        if (current == null || !current.getStartEt().equals(candleStart)) {
            if (current != null) checkBreakout(symbol, current.getClose(), levels, CandleType.FIVE_MINUTE);
            currentFiveMinuteCandleBySymbol.put(symbol, new FiveMinuteCandle(candleStart, price));
        } else {
            current.update(price);
        }
    }

    private void processFifteenMinuteCandle(String symbol, double price, ZonedDateTime tickTimeCst, PremarketLevels levels) {
        int intervalStart = (tickTimeCst.getMinute() / 15) * 15;
        ZonedDateTime candleStart = tickTimeCst.withMinute(intervalStart).withSecond(0).withNano(0);
        FifteenMinuteCandle current = currentFifteenMinuteCandleBySymbol.get(symbol);
        if (current == null || !current.getStartEt().equals(candleStart)) {
            if (current != null) checkBreakout(symbol, current.getClose(), levels, CandleType.FIFTEEN_MINUTE);
            currentFifteenMinuteCandleBySymbol.put(symbol, new FifteenMinuteCandle(candleStart, price));
        } else {
            current.update(price);
        }
    }

    private void checkBreakout(String symbol, double close, PremarketLevels levels, CandleType candleType) {
        double preHigh = levels.getHigh();
        double preLow = levels.getLow();
        
        // Debug logging
        System.out.println("🔍 [" + ZonedDateTime.now(CHICAGO).toLocalTime() + "] " + symbol + 
            " | Candle: " + candleType +
            " | Close: $" + String.format("%.2f", close) + 
            " | PreHigh: $" + String.format("%.2f", preHigh) + 
            " | PreLow: $" + String.format("%.2f", preLow));
        
        // Check for BUY signal (price above premarket high)
        if (close > preHigh && preHigh > 0) {
            if (!hasTriggered(symbol, Direction.BULLISH, candleType)) {
                System.out.println("🚨✅ BUY SIGNAL: " + symbol + " (" + candleType + ") closed at $" + close + " (above preHigh $" + preHigh + ")");
                markTriggered(symbol, Direction.BULLISH, candleType);
                String candleTypeStr = candleType == CandleType.ONE_MINUTE ? "ONE_MIN" : 
                                       (candleType == CandleType.FIVE_MINUTE ? "FIVE_MIN" : "FIFTEEN_MIN");
                alertBroadcastService.sendBreakoutAlert(symbol, "HIGH", candleTypeStr);
            }
            return;
        }
        
        // Check for SELL signal (price below premarket low)
        if (close < preLow && preLow > 0) {
            if (!hasTriggered(symbol, Direction.BEARISH, candleType)) {
                System.out.println("🚨❌ SELL SIGNAL: " + symbol + " (" + candleType + ") closed at $" + close + " (below preLow $" + preLow + ")");
                markTriggered(symbol, Direction.BEARISH, candleType);
                String candleTypeStr = candleType == CandleType.ONE_MINUTE ? "ONE_MIN" : 
                                       (candleType == CandleType.FIVE_MINUTE ? "FIVE_MIN" : "FIFTEEN_MIN");
                alertBroadcastService.sendBreakoutAlert(symbol, "LOW", candleTypeStr);
            }
            return;
        }
    }

    /**
     * Get all active breakouts for dashboard display
     */
    public List<BreakoutInfo> getActiveBreakoutsForDashboard() {
        ZonedDateTime nowEt = ZonedDateTime.now(NEW_YORK);
        if (nowEt.toLocalTime().isAfter(DASHBOARD_CUTOFF_ET)) {
            return Collections.emptyList();
        }
        
        List<BreakoutInfo> allBreakouts = new ArrayList<>();
        
        for (Map.Entry<String, Set<String>> entry : triggeredBreakouts.entrySet()) {
            String symbol = entry.getKey();
            PremarketLevels levels = premarketLevelsService.getLevels(symbol);
            double preHigh = levels != null ? levels.getHigh() : 0;
            double preLow = levels != null ? levels.getLow() : 0;
            
            for (String triggerKey : entry.getValue()) {
                String[] parts = triggerKey.split("_");
                if (parts.length >= 3) {
                    Direction direction = Direction.valueOf(parts[1]);
                    CandleType candleType = CandleType.valueOf(parts[2]);
                    
                    BreakoutInfo info = new BreakoutInfo(
                        symbol, direction, ZonedDateTime.now(NEW_YORK),
                        preHigh, preLow, candleType, 0
                    );
                    allBreakouts.add(info);
                }
            }
        }
        return allBreakouts;
    }

    public void resetForNextSession() {
        currentOneMinuteCandleBySymbol.clear();
        currentFiveMinuteCandleBySymbol.clear();
        currentFifteenMinuteCandleBySymbol.clear();
        triggeredBreakouts.clear();
        System.out.println("🔄 Breakout engine reset for next session");
    }
}