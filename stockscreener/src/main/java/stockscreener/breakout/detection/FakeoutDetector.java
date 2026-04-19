package stockscreener.breakout.detection;

/**
 * Detects fakeouts — situations where price briefly breaks PMH/PML but
 * immediately reverses back inside the range.
 *
 * Institutional fakeout rules:
 *  - Price must NOT return inside PMH/PML within the confirmation window.
 *  - Candle body must be meaningful (not a tiny wick breakout).
 *  - VWAP must support the breakout direction.
 *  - Volume must support the breakout direction.
 *
 * This class is intentionally pure and contains no Spring dependencies.
 */
public class FakeoutDetector {

    // Minimum candle body size as % of PM range to be considered meaningful
    private static final double MIN_BODY_PERCENT_OF_RANGE = 10.0; // 10%

    /**
     * Detects whether a breakout is a fakeout.
     *
     * @param lastPrice       Latest price after breakout
     * @param pmHigh          Premarket high
     * @param pmLow           Premarket low
     * @param candleOpen      Candle open price
     * @param candleClose     Candle close price
     * @param vwap            Current VWAP
     * @param volume          Current candle volume
     * @param avgVolume       Average 1-minute volume
     * @param direction       "BREAKOUT_UP" or "BREAKOUT_DOWN"
     */
    public FakeoutResult detect(Double lastPrice,
                                Double pmHigh,
                                Double pmLow,
                                Double candleOpen,
                                Double candleClose,
                                Double vwap,
                                Long volume,
                                Long avgVolume,
                                String direction) {

        if (lastPrice == null || pmHigh == null || pmLow == null ||
            candleOpen == null || candleClose == null) {
            return FakeoutResult.fakeout("Missing price or premarket range");
        }

        double pmRange = pmHigh - pmLow;
        if (pmRange <= 0) {
            return FakeoutResult.fakeout("Invalid premarket range");
        }

        // 1. Price returned inside PM range → fakeout
        if (lastPrice <= pmHigh && lastPrice >= pmLow) {
            return FakeoutResult.fakeout("Price returned inside PM range");
        }

        // 2. Candle body too small → wick breakout → fakeout
        double bodySize = Math.abs(candleClose - candleOpen);
        double bodyPercent = (bodySize / pmRange) * 100.0;

        if (bodyPercent < MIN_BODY_PERCENT_OF_RANGE) {
            return FakeoutResult.fakeout(
                    "Candle body too small (" + String.format("%.2f", bodyPercent) + "%)"
            );
        }

        // 3. VWAP contradiction → fakeout
        if (vwap != null) {
            if ("BREAKOUT_UP".equals(direction) && candleClose < vwap) {
                return FakeoutResult.fakeout("VWAP contradicts BREAKOUT_UP");
            }
            if ("BREAKOUT_DOWN".equals(direction) && candleClose > vwap) {
                return FakeoutResult.fakeout("VWAP contradicts BREAKOUT_DOWN");
            }
        }

        // 4. Weak volume → fakeout
        if (volume != null && avgVolume != null && avgVolume > 0) {
            if (volume < avgVolume) {
                return FakeoutResult.fakeout(
                        "Volume too weak (" + volume + " < avg=" + avgVolume + ")"
                );
            }
        }

        // Passed all fakeout checks
        return FakeoutResult.valid();
    }

    /**
     * Immutable result object for fakeout detection.
     */
    public static final class FakeoutResult {
        private final boolean valid;
        private final String reason;

        private FakeoutResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static FakeoutResult valid() {
            return new FakeoutResult(true, null);
        }

        public static FakeoutResult fakeout(String reason) {
            return new FakeoutResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
