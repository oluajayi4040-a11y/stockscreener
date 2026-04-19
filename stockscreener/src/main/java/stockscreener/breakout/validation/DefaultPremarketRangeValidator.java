package stockscreener.breakout.validation;

/**
 * Institutional-grade implementation of PremarketRangeValidator.
 *
 * This class enforces strict validation rules to ensure that only symbols with
 * meaningful and tradeable premarket ranges are scanned for breakouts.
 *
 * Rules:
 *  - PM High must be > PM Low
 *  - PM range must exceed a minimum percentage threshold
 *  - PM volume must exceed a minimum threshold
 *  - No missing or invalid values
 */
public class DefaultPremarketRangeValidator implements PremarketRangeValidator {

    // Minimum acceptable premarket range as a percentage of previous close
    private static final double MIN_RANGE_PERCENT = 0.20; // 0.20%

    // Minimum acceptable premarket volume
    private static final long MIN_PREMARKET_VOLUME = 20_000L;

    @Override
    public boolean isValid(PremarketRange range) {
        return validate(range).isValid();
    }

    @Override
    public PremarketRangeValidationResult validate(PremarketRange range) {

        if (range == null) {
            return PremarketRangeValidationResult.invalid("PremarketRange is null");
        }

        if (range.getSymbol() == null || range.getSymbol().isBlank()) {
            return PremarketRangeValidationResult.invalid("Symbol is missing");
        }

        Double pmHigh = range.getPremarketHigh();
        Double pmLow = range.getPremarketLow();
        Long pmVolume = range.getPremarketVolume();
        Double prevClose = range.getPreviousClose();

        // Missing values
        if (pmHigh == null || pmLow == null) {
            return PremarketRangeValidationResult.invalid("Premarket high/low is missing");
        }

        if (prevClose == null || prevClose <= 0) {
            return PremarketRangeValidationResult.invalid("Previous close is missing or invalid");
        }

        if (pmVolume == null) {
            return PremarketRangeValidationResult.invalid("Premarket volume is missing");
        }

        // PM High must be > PM Low
        if (pmHigh <= pmLow) {
            return PremarketRangeValidationResult.invalid(
                    "Premarket high must be greater than premarket low");
        }

        // Range must exceed minimum threshold
        double rangePercent = ((pmHigh - pmLow) / prevClose) * 100.0;
        if (rangePercent < MIN_RANGE_PERCENT) {
            return PremarketRangeValidationResult.invalid(
                    "Premarket range too small (" + String.format("%.3f", rangePercent) + "%)");
        }

        // Volume threshold
        if (pmVolume < MIN_PREMARKET_VOLUME) {
            return PremarketRangeValidationResult.invalid(
                    "Premarket volume too low (" + pmVolume + ")");
        }

        // All checks passed
        return PremarketRangeValidationResult.ok();
    }
}
