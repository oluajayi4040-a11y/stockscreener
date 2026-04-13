package stockscreener.model;

public class PremarketLevels {

    private double high;
    private double low;
    private double open;  // ⭐ NEW: premarket open price (first trade of premarket)

    // Original constructor (for backward compatibility)
    public PremarketLevels(double high, double low) {
        this.high = high;
        this.low = low;
        this.open = 0.0;
    }

    // ⭐ NEW constructor with open price
    public PremarketLevels(double high, double low, double open) {
        this.high = high;
        this.low = low;
        this.open = open;
    }

    // Getters
    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getOpen() {
        return open;
    }

    // Setters
    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    @Override
    public String toString() {
        return "PremarketLevels{" +
                "high=" + high +
                ", low=" + low +
                ", open=" + open +
                '}';
    }
}