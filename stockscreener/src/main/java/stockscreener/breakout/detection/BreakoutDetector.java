package stockscreener.breakout.detection;

import java.time.LocalDateTime;

/**
 * Core breakout detection logic.
 *
 * Institutional rules:
 *  - Candle BODY must close outside PMH/PML.
 *  - Wicks do NOT count.
 *  - Breakout must be confirmed by:
 *        (A) Holding outside the range for a minimum duration, OR
 *        (B) Next tick confirming the breakout.
 *
 * This class is intentionally pure and contains no Spring dependencies.
 */
public class BreakoutDetector {

    // How long price must stay outside PMH/PML to confirm breakout (in seconds)
    private static final int HOLD_CONFIRMATION_SECONDS = 10;

    /**
     * Determines whether a candle has broken out of the premarket range.
     *
     * @param closePrice   The candle close price
     * @param openPrice    The candle open price
     * @param pmHigh       Premarket high
     * @param pmLow        Premarket low
     */
    public BreakoutResult detect(Double openPrice,
                                 Double closePrice,
                                 Double pmHigh,
                                 Double pmLow) {

        if (openPrice == null || closePrice == null || pmHigh == null || pmLow == null) {
            return BreakoutResult.invalid("Missing price or premarket range");
        }

        // Candle body must close ABOVE PMH
        if (closePrice > pmHigh && openPrice >= pmLow) {
            return BreakoutResult.breakoutUp();
        }

        // Candle body must close BELOW PML
        if (closePrice < pmLow && openPrice <= pmHigh) {
            return BreakoutResult.breakoutDown();
        }

        return BreakoutResult.noBreakout();
    }

    /**
     * Confirms breakout by requiring price to remain outside PMH/PML.
     *
     * @param breakoutTime   Time breakout was detected
     * @param currentTime    Current time
     * @param lastPrice      Latest price
     * @param pmHigh         Premarket high
     * @param pmLow          Premarket low
     */
    public boolean confirmBreakout(LocalDateTime breakoutTime,
                                   LocalDateTime currentTime,
                                   Double lastPrice,
                                   Double pmHigh,
                                   Double pmLow) {

        if (breakoutTime == null || currentTime == null || lastPrice == null) {
            return false;
        }

        long secondsElapsed = java.time.Duration.between(breakoutTime, currentTime).getSeconds();

        if (secondsElapsed < HOLD_CONFIRMATION_SECONDS) {
            return false;
        }

        // Must still be outside the range
        return lastPrice > pmHigh || lastPrice < pmLow;
    }

    /**
     * Immutable breakout result object.
     */
    public static final class BreakoutResult {
        private final boolean breakout;
        private final String direction; // BREAKOUT_UP, BREAKOUT_DOWN, NONE
        private final String reason;

        private BreakoutResult(boolean breakout, String direction, String reason) {
            this.breakout = breakout;
            this.direction = direction;
            this.reason = reason;
        }

        public static BreakoutResult breakoutUp() {
            return new BreakoutResult(true, "BREAKOUT_UP", null);
        }

        public static BreakoutResult breakoutDown() {
            return new BreakoutResult(true, "BREAKOUT_DOWN", null);
        }

        public static BreakoutResult noBreakout() {
            return new BreakoutResult(false, "NONE", null);
        }

        public static BreakoutResult invalid(String reason) {
            return new BreakoutResult(false, "NONE", reason);
        }

        public boolean isBreakout() {
            return breakout;
        }

        public String getDirection() {
            return direction;
        }

        public String getReason() {
            return reason;
        }
    }
}
