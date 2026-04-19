package stockscreener.model;

public class QualifiedSignal {

    private String symbol;
    private String companyName;

    private double lastPrice;

    private double premarketHigh;
    private double premarketLow;
    private double premarketOpen;
    private double premarketVolume;

    private double currentVolume;
    private double averageVolume;

    private double vwap;

    private String direction; // BREAKOUT or BREAKDOWN
    private long timestamp;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public double getPremarketHigh() {
        return premarketHigh;
    }

    public void setPremarketHigh(double premarketHigh) {
        this.premarketHigh = premarketHigh;
    }

    public double getPremarketLow() {
        return premarketLow;
    }

    public void setPremarketLow(double premarketLow) {
        this.premarketLow = premarketLow;
    }

    public double getPremarketOpen() {
        return premarketOpen;
    }

    public void setPremarketOpen(double premarketOpen) {
        this.premarketOpen = premarketOpen;
    }

    public double getPremarketVolume() {
        return premarketVolume;
    }

    public void setPremarketVolume(double premarketVolume) {
        this.premarketVolume = premarketVolume;
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(double currentVolume) {
        this.currentVolume = currentVolume;
    }

    public double getAverageVolume() {
        return averageVolume;
    }

    public void setAverageVolume(double averageVolume) {
        this.averageVolume = averageVolume;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
