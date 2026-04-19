package stockscreener.breakout.engine;

import stockscreener.breakout.validation.*;
import stockscreener.breakout.detection.*;
import stockscreener.breakout.scoring.*;

import java.time.LocalDateTime;

/**
 * The SignalEngine orchestrates the entire institutional breakout pipeline:
 *
 * 1. Premarket range validation
 * 2. Breakout window validation
 * 3. Breakout detection (candle body close outside PMH/PML)
 * 4. Fakeout detection (reject wick breakouts, reversals, weak setups)
 * 5. VWAP confirmation
 * 6. Volume confirmation
 * 7. Breakout strength scoring (0–100)
 *
 * Produces a final BreakoutSignal object.
 *
 * This class is intentionally pure and contains no Spring dependencies.
 */
public class SignalEngine {

    private final PremarketRangeValidator premarketValidator;
    private final BreakoutWindowValidator windowValidator;
    private final BreakoutDetector breakoutDetector;
    private final FakeoutDetector fakeoutDetector;
    private final BreakoutStrengthScorer strengthScorer;

    public SignalEngine() {
        this.premarketValidator = new DefaultPremarketRangeValidator();
        this.windowValidator = new BreakoutWindowValidator();
        this.breakoutDetector = new BreakoutDetector();
        this.fakeoutDetector = new FakeoutDetector();
        this.strengthScorer = new BreakoutStrengthScorer();
    }

    /**
     * Runs the full institutional breakout pipeline.
     */
    public BreakoutSignal evaluate(
            String symbol,
            Double openPrice,
            Double closePrice,
            Double lastPrice,
            Double pmHigh,
            Double pmLow,
            Double vwap,
            Long volume,
            Long avgVolume,
            Long pmVolume,
            Double previousClose,
            LocalDateTime timestamp
    ) {

        // 1. Premarket range validation
        PremarketRangeValidator.PremarketRange pmRange =
                PremarketRangeValidator.PremarketRange.builder(symbol)
                        .premarketHigh(pmHigh)
                        .premarketLow(pmLow)
                        .premarketVolume(pmVolume)
                        .previousClose(previousClose)
                        .build();

        var pmValidation = premarketValidator.validate(pmRange);
        if (!pmValidation.isValid()) {
            return BreakoutSignal.rejected(symbol, "Premarket invalid: " + pmValidation.getReason());
        }

        // 2. Breakout window validation
        var windowValidation = windowValidator.validate(timestamp);
        if (!windowValidation.isValid()) {
            return BreakoutSignal.rejected(symbol, "Outside breakout window");
        }

        // 3. Breakout detection
        var breakoutResult = breakoutDetector.detect(openPrice, closePrice, pmHigh, pmLow);
        if (!breakoutResult.isBreakout()) {
            return BreakoutSignal.noBreakout(symbol);
        }

        String direction = breakoutResult.getDirection();

        // 4. Fakeout detection
        var fakeoutResult = fakeoutDetector.detect(
                lastPrice,
                pmHigh,
                pmLow,
                openPrice,
                closePrice,
                vwap,
                volume,
                avgVolume,
                direction
        );

        if (!fakeoutResult.isValid()) {
            return BreakoutSignal.rejected(symbol, "Fakeout: " + fakeoutResult.getReason());
        }

        // 5. VWAP confirmation
        var vwapValidation = new VWAPValidator().validate(lastPrice, vwap, direction);
        if (!vwapValidation.isValid()) {
            return BreakoutSignal.rejected(symbol, "VWAP reject: " + vwapValidation.getReason());
        }

        // 6. Volume confirmation
        var volumeValidation = new VolumeValidator().validate(volume, avgVolume);
        if (!volumeValidation.isValid()) {
            return BreakoutSignal.rejected(symbol, "Volume reject: " + volumeValidation.getReason());
        }

        // 7. Strength scoring
        var scoreResult = strengthScorer.score(
                openPrice,
                closePrice,
                pmHigh,
                pmLow,
                vwap,
                volume,
                avgVolume,
                direction
        );

        if (!scoreResult.isValid()) {
            return BreakoutSignal.rejected(symbol, "Scoring error: " + scoreResult.getReason());
        }

        int score = scoreResult.getScore();

        return BreakoutSignal.success(
                symbol,
                direction,
                lastPrice,
                pmHigh,
                pmLow,
                vwap,
                volume,
                score,
                timestamp
        );
    }

    /**
     * Immutable breakout signal result.
     */
    public static final class BreakoutSignal {
        private final boolean breakout;
        private final boolean accepted;
        private final String symbol;
        private final String direction;
        private final Double price;
        private final Double pmHigh;
        private final Double pmLow;
        private final Double vwap;
        private final Long volume;
        private final Integer strengthScore;
        private final String reason;
        private final LocalDateTime timestamp;

        private BreakoutSignal(boolean breakout,
                               boolean accepted,
                               String symbol,
                               String direction,
                               Double price,
                               Double pmHigh,
                               Double pmLow,
                               Double vwap,
                               Long volume,
                               Integer strengthScore,
                               String reason,
                               LocalDateTime timestamp) {

            this.breakout = breakout;
            this.accepted = accepted;
            this.symbol = symbol;
            this.direction = direction;
            this.price = price;
            this.pmHigh = pmHigh;
            this.pmLow = pmLow;
            this.vwap = vwap;
            this.volume = volume;
            this.strengthScore = strengthScore;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        public static BreakoutSignal success(String symbol,
                                             String direction,
                                             Double price,
                                             Double pmHigh,
                                             Double pmLow,
                                             Double vwap,
                                             Long volume,
                                             Integer score,
                                             LocalDateTime timestamp) {

            return new BreakoutSignal(
                    true,
                    true,
                    symbol,
                    direction,
                    price,
                    pmHigh,
                    pmLow,
                    vwap,
                    volume,
                    score,
                    null,
                    timestamp
            );
        }

        public static BreakoutSignal rejected(String symbol, String reason) {
            return new BreakoutSignal(
                    false,
                    false,
                    symbol,
                    "NONE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    reason,
                    null
            );
        }

        public static BreakoutSignal noBreakout(String symbol) {
            return new BreakoutSignal(
                    false,
                    true,
                    symbol,
                    "NONE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        public boolean isBreakout() { return breakout; }
        public boolean isAccepted() { return accepted; }
        public String getSymbol() { return symbol; }
        public String getDirection() { return direction; }
        public Double getPrice() { return price; }
        public Double getPmHigh() { return pmHigh; }
        public Double getPmLow() { return pmLow; }
        public Double getVwap() { return vwap; }
        public Long getVolume() { return volume; }
        public Integer getStrengthScore() { return strengthScore; }
        public String getReason() { return reason; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
