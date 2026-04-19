package stockscreener.breakout.scoring;

/**
 * Scores breakout strength on a 0–100 scale using institutional factors:
 *
 * Factors:
 *  - Distance from PMH/PML (bigger = stronger)
 *  - Candle body size relative to PM range
 *  - Volume strength (vs average)
 *  - VWAP distance (further = stronger)
 *  - Momentum (close > open for breakout up, close < open for breakout down)
 *
 * This class is intentionally pure and contains no Spring dependencies.
 */
public class BreakoutStrengthScorer {

    /**
     * Calculates a 0–100 breakout strength score.
     *
     * @param openPrice        Candle open
     * @param closePrice       Candle close
     * @param pmHigh           Premarket high
     * @param pmLow            Premarket low
     * @param vwap             Current VWAP
     * @param volume           Current candle volume
     * @param avgVolume        Average 1-minute volume
     * @param direction        "BREAKOUT_UP" or "BREAKOUT_DOWN"
     */
    public ScoreResult score(Double openPrice,
                             Double closePrice,
                             Double pmHigh,
                             Double pmLow,
                             Double vwap,
                             Long volume,
                             Long avgVolume,
                             String direction) {

        if (openPrice == null || closePrice == null ||
            pmHigh == null || pmLow == null || direction == null) {
            return ScoreResult.invalid("Missing required values");
        }

        double pmRange = pmHigh - pmLow;
        if (pmRange <= 0) {
            return ScoreResult.invalid("Invalid premarket range");
        }

        double score = 0.0;

        // 1. Distance from PMH/PML (max 30 points)
        double distancePoints = calculateDistancePoints(closePrice, pmHigh, pmLow, pmRange, direction);
        score += distancePoints;

        // 2. Candle body size (max 20 points)
        double bodyPoints = calculateBodyPoints(openPrice, closePrice, pmRange);
        score += bodyPoints;

        // 3. Volume strength (max 25 points)
        double volumePoints = calculateVolumePoints(volume, avgVolume);
        score += volumePoints;

        // 4. VWAP distance (max 15 points)
        double vwapPoints = calculateVWAPPoints(closePrice, vwap, direction);
        score += vwapPoints;

        // 5. Momentum (max 10 points)
        double momentumPoints = calculateMomentumPoints(openPrice, closePrice, direction);
        score += momentumPoints;

        // Clamp score to 0–100
        score = Math.max(0, Math.min(100, score));

        return ScoreResult.valid((int) score);
    }

    private double calculateDistancePoints(Double close, Double pmHigh, Double pmLow,
                                           double pmRange, String direction) {

        if ("BREAKOUT_UP".equals(direction)) {
            double dist = close - pmHigh;
            return Math.min(30.0, (dist / pmRange) * 30.0);
        }

        if ("BREAKOUT_DOWN".equals(direction)) {
            double dist = pmLow - close;
            return Math.min(30.0, (dist / pmRange) * 30.0);
        }

        return 0.0;
    }

    private double calculateBodyPoints(Double open, Double close, double pmRange) {
        double body = Math.abs(close - open);
        double percent = (body / pmRange) * 100.0;

        if (percent >= 20) return 20.0;
        if (percent >= 10) return 15.0;
        if (percent >= 5)  return 10.0;
        if (percent >= 2)  return 5.0;

        return 0.0;
    }

    private double calculateVolumePoints(Long volume, Long avgVolume) {
        if (volume == null || avgVolume == null || avgVolume <= 0) {
            return 0.0;
        }

        double ratio = (double) volume / avgVolume;

        if (ratio >= 3.0) return 25.0;
        if (ratio >= 2.0) return 20.0;
        if (ratio >= 1.5) return 15.0;
        if (ratio >= 1.0) return 10.0;

        return 0.0;
    }

    private double calculateVWAPPoints(Double close, Double vwap, String direction) {
        if (vwap == null) return 0.0;

        double dist = Math.abs(close - vwap);

        if ("BREAKOUT_UP".equals(direction) && close > vwap) {
            return Math.min(15.0, dist * 2.0);
        }

        if ("BREAKOUT_DOWN".equals(direction) && close < vwap) {
            return Math.min(15.0, dist * 2.0);
        }

        return 0.0;
    }

    private double calculateMomentumPoints(Double open, Double close, String direction) {
        if ("BREAKOUT_UP".equals(direction) && close > open) {
            return 10.0;
        }

        if ("BREAKOUT_DOWN".equals(direction) && close < open) {
            return 10.0;
        }

        return 0.0;
    }

    /**
     * Immutable result object for scoring.
     */
    public static final class ScoreResult {
        private final boolean valid;
        private final Integer score;
        private final String reason;

        private ScoreResult(boolean valid, Integer score, String reason) {
            this.valid = valid;
            this.score = score;
            this.reason = reason;
        }

        public static ScoreResult valid(int score) {
            return new ScoreResult(true, score, null);
        }

        public static ScoreResult invalid(String reason) {
            return new ScoreResult(false, null, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public Integer getScore() {
            return score;
        }

        public String getReason() {
            return reason;
        }
    }
}
