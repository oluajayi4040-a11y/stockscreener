package stockscreener.breakout.validation;

/**
 * Validates whether a symbol's premarket range is usable for breakout logic.
 *
 * This interface defines the contract for validating premarket data before
 * running breakout detection. It is intentionally pure and contains no Spring,
 * HTTP, or database dependencies.
 *
 * The implementing class (DefaultPremarketRangeValidator) will contain the
 * institutional-grade rules:
 *  - PM High > PM Low
 *  - PM range > minimum threshold
 *  - PM volume > minimum threshold
 *  - No missing or invalid values
 */
public interface PremarketRangeValidator {

    /**
     * Returns true if the premarket range is valid for breakout scanning.
     */
    boolean isValid(PremarketRange range);

    /**
     * Returns a detailed validation result with reason when invalid.
     */
    PremarketRangeValidationResult validate(PremarketRange range);

    /**
     * Immutable value object representing the premarket range for a symbol.
     */
    final class PremarketRange {
        private final String symbol;
        private final Double premarketHigh;
        private final Double premarketLow;
        private final Long premarketVolume;
        private final Double previousClose;

        private PremarketRange(Builder builder) {
            this.symbol = builder.symbol;
            this.premarketHigh = builder.premarketHigh;
            this.premarketLow = builder.premarketLow;
            this.premarketVolume = builder.premarketVolume;
            this.previousClose = builder.previousClose;
        }

        public String getSymbol() {
            return symbol;
        }

        public Double getPremarketHigh() {
            return premarketHigh;
        }

        public Double getPremarketLow() {
            return premarketLow;
        }

        public Long getPremarketVolume() {
            return premarketVolume;
        }

        public Double getPreviousClose() {
            return previousClose;
        }

        public static Builder builder(String symbol) {
            return new Builder(symbol);
        }

        /**
         * Builder for PremarketRange.
         */
        public static final class Builder {
            private final String symbol;
            private Double premarketHigh;
            private Double premarketLow;
            private Long premarketVolume;
            private Double previousClose;

            public Builder(String symbol) {
                this.symbol = symbol;
            }

            public Builder premarketHigh(Double premarketHigh) {
                this.premarketHigh = premarketHigh;
                return this;
            }

            public Builder premarketLow(Double premarketLow) {
                this.premarketLow = premarketLow;
                return this;
            }

            public Builder premarketVolume(Long premarketVolume) {
                this.premarketVolume = premarketVolume;
                return this;
            }

            public Builder previousClose(Double previousClose) {
                this.previousClose = previousClose;
                return this;
            }

            public PremarketRange build() {
                return new PremarketRange(this);
            }
        }
    }

    /**
     * Immutable result describing whether the premarket range is valid and, if not, why.
     */
    final class PremarketRangeValidationResult {
        private final boolean valid;
        private final String reason;

        private PremarketRangeValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static PremarketRangeValidationResult ok() {
            return new PremarketRangeValidationResult(true, null);
        }

        public static PremarketRangeValidationResult invalid(String reason) {
            return new PremarketRangeValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
