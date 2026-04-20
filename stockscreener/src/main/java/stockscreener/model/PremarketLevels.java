package stockscreener.model;

/**
 * Represents premarket levels for a symbol:
 *  - Premarket High
 *  - Premarket Low
 *  - Premarket Open (first premarket candle)
 *  - Premarket Volume (sum of all premarket bars)
 *  - Previous Close (yesterday's close)
 */
public class PremarketLevels {

    private double high;
    private double low;
    private double open;

    private long premarketVolume;     // FIXED: long (matches Alpaca)
    private Double previousClose;     // FIXED: nullable wrapper

    public PremarketLevels() {
    }

    /**
     * Full institutional constructor.
     * Matches AlpacaMarketDataClient usage:
     *
     * new PremarketLevels(high, low, volume, previousClose)
     */
    public PremarketLevels(double high, double low, long premarketVolume, Double previousClose) {
        this.high = high;
        this.low = low;
        this.premarketVolume = premarketVolume;
        this.previousClose = previousClose;
    }

    // -------------------
    // Getters
    // -------------------

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getOpen() {
        return open;
    }

    public long getPremarketVolume() {
        return premarketVolume;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    // -------------------
    // Setters
    // -------------------

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setPremarketVolume(long premarketVolume) {
        this.premarketVolume = premarketVolume;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }

    @Override
    public String toString() {
        return "PremarketLevels{" +
                "high=" + high +
                ", low=" + low +
                ", open=" + open +
                ", premarketVolume=" + premarketVolume +
                ", previousClose=" + previousClose +
                '}';
    }
}
