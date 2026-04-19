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

    private double premarketVolume;   // NEW
    private Double previousClose;     // NEW

    public PremarketLevels() {
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getPremarketVolume() {
        return premarketVolume;
    }

    public void setPremarketVolume(double premarketVolume) {
        this.premarketVolume = premarketVolume;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }
}
