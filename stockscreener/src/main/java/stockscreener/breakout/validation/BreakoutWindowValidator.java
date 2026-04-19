package stockscreener.breakout.validation;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Validates whether the current time is within the institutional breakout window.
 *
 * Institutional rule:
 *  - Breakout window = 9:30 AM → 9:50 AM Eastern Time
 *  - This matches ScannerScheduler and SignalEngine expectations.
 *
 * This validator is intentionally pure and contains no Spring dependencies.
 */
public class BreakoutWindowValidator {

    // Market timezone (US equities)
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    // Institutional breakout window
    private static final LocalTime START_ET = LocalTime.of(9, 30);
    private static final LocalTime END_ET   = LocalTime.of(9, 50);

    /**
     * Returns true if the given timestamp is within the breakout window.
     */
    public boolean isWithinBreakoutWindow(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }

        LocalTime nyTime = timestamp
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(NEW_YORK)
                .toLocalTime();

        return !nyTime.isBefore(START_ET) && !nyTime.isAfter(END_ET);
    }

    /**
     * Returns a detailed validation result.
     */
    public WindowValidationResult validate(LocalDateTime timestamp) {
        if (timestamp == null) {
            return WindowValidationResult.invalid("Timestamp is null");
        }

        if (isWithinBreakoutWindow(timestamp)) {
            return WindowValidationResult.ok();
        }

        return WindowValidationResult.invalid(
                "Outside breakout window (valid: 9:30–9:50 AM ET)"
        );
    }

    /**
     * Immutable result object for window validation.
     */
    public static final class WindowValidationResult {
        private final boolean valid;
        private final String reason;

        private WindowValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static WindowValidationResult ok() {
            return new WindowValidationResult(true, null);
        }

        public static WindowValidationResult invalid(String reason) {
            return new WindowValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
