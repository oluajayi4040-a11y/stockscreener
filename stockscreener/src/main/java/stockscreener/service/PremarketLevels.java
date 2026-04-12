package stockscreener.service;

public class PremarketLevels {

    private final double high;
    private final double low;

    public PremarketLevels(double high, double low) {
        this.high = high;
        this.low = low;
    }

    // ⭐ Updated to match WatchlistService usage
    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }
}
