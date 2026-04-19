package stockscreener.breakout.validation;

/**
 * Validates whether a breakout candle has sufficient volume to be considered valid.
 *
 * Institutional rule:
 *  - Breakouts must occur on strong volume.
 *  - Volume must be either:
 *      (A) Above the average 1-minute volume, OR
 *      (B) Above a minimum absolute threshold.
 *
 * This validator is intentionally pure and contains no Spring dependencies.
 */
public class VolumeValidator {

    // Minimum absolute volume threshold for a breakout candle
    private static final long MIN_VOLUME_THRESHOLD = 50_000L;

    /**
     * Validates whether the breakout candle has sufficient volume.
     *
     * @param currentVolume      Volume of the breakout candle
     * @param average1MinVolume  Average 1-minute volume for the symbol
     */
    public VolumeValidationResult validate(Long currentVolume, Long average1MinVolume) {

        if (currentVolume == null) {
            return VolumeValidationResult.invalid("Current volume is missing");
        }

        if (currentVolume <= 0) {
            return VolumeValidationResult.invalid("Current volume is invalid");
        }

        // Rule A: Above average 1-minute volume
        if (average1MinVolume != null && average1MinVolume > 0) {
            if (currentVolume > average1MinVolume) {
                return VolumeValidationResult.ok();
            }
        }

        // Rule B: Above minimum threshold
        if (currentVolume >= MIN_VOLUME_THRESHOLD) {
            return VolumeValidationResult.ok();
        }

        return VolumeValidationResult.invalid(
                "Volume too weak (" + currentVolume +
                " < avg=" + average1MinVolume +
                ", min=" + MIN_VOLUME_THRESHOLD + ")"
        );
    }

    /**
     * Immutable result object for volume validation.
     */
    public static final class VolumeValidationResult {
        private final boolean valid;
        private final String reason;

        private VolumeValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static VolumeValidationResult ok() {
            return new VolumeValidationResult(true, null);
        }

        public static VolumeValidationResult invalid(String reason) {
            return new VolumeValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
