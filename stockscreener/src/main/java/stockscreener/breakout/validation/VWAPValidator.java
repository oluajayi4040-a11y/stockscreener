package stockscreener.breakout.validation;

/**
 * Validates whether a breakout direction is confirmed by VWAP.
 *
 * Institutional rule:
 *  - BREAKOUT_UP  → last price must be ABOVE VWAP
 *  - BREAKOUT_DOWN → last price must be BELOW VWAP
 *
 * This validator is intentionally pure and contains no Spring dependencies.
 */
public class VWAPValidator {

    /**
     * Validates VWAP confirmation for a breakout.
     *
     * @param lastPrice  The most recent trade price.
     * @param vwap       The current VWAP value.
     * @param direction  "BREAKOUT_UP" or "BREAKOUT_DOWN".
     */
    public VWAPValidationResult validate(Double lastPrice, Double vwap, String direction) {

        if (lastPrice == null || vwap == null) {
            return VWAPValidationResult.invalid("Missing last price or VWAP");
        }

        if (direction == null) {
            return VWAPValidationResult.invalid("Missing breakout direction");
        }

        switch (direction) {

            case "BREAKOUT_UP":
                if (lastPrice > vwap) {
                    return VWAPValidationResult.ok();
                }
                return VWAPValidationResult.invalid(
                        "BREAKOUT_UP rejected: price below VWAP (" + lastPrice + " < " + vwap + ")"
                );

            case "BREAKOUT_DOWN":
                if (lastPrice < vwap) {
                    return VWAPValidationResult.ok();
                }
                return VWAPValidationResult.invalid(
                        "BREAKOUT_DOWN rejected: price above VWAP (" + lastPrice + " > " + vwap + ")"
                );

            default:
                return VWAPValidationResult.invalid("Unknown breakout direction: " + direction);
        }
    }

    /**
     * Immutable result object for VWAP validation.
     */
    public static final class VWAPValidationResult {
        private final boolean valid;
        private final String reason;

        private VWAPValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static VWAPValidationResult ok() {
            return new VWAPValidationResult(true, null);
        }

        public static VWAPValidationResult invalid(String reason) {
            return new VWAPValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
