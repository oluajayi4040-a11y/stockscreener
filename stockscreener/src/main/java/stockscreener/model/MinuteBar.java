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

    private double volume;

    public MinuteBar() {
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
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

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }
}
