package stockscreener.model;

import java.time.ZonedDateTime;

/**
 * Represents a single 1-minute bar from Alpaca Market Data.
 */
public class MinuteBar {

    private ZonedDateTime timestamp;

    private double open;
    private double high;
    private double low;
    private double close;

    private long volume;   // FIXED: long (matches Alpaca + your engine)

    public MinuteBar() {
    }

    /**
     * Full institutional constructor.
     * Matches AlpacaMarketDataClient usage:
     *
     * new MinuteBar(o, h, l, c, v, timestamp)
     */
    public MinuteBar(double open,
                     double high,
                     double low,
                     double close,
                     long volume,
                     ZonedDateTime timestamp) {

        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    // -------------------
    // Getters
    // -------------------

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    // -------------------
    // Setters
    // -------------------

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "MinuteBar{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}
