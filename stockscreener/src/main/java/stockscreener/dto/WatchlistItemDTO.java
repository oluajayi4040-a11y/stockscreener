package stockscreener.dto;

public class WatchlistItemDTO {

    private String symbol;
    private double price;
    private double premarketHigh;
    private double premarketLow;
    private double previousClose;
    private double marketOpen;

    // ⭐ Breakout fields
    private boolean hasBreakout;
    private String breakoutDirection;
    private String breakoutTime;

    // Constructor
    public WatchlistItemDTO(String symbol,
                            double price,
                            double premarketHigh,
                            double premarketLow,
                            double previousClose,
                            double marketOpen) {
        this.symbol = symbol;
        this.price = price;
        this.premarketHigh = premarketHigh;
        this.premarketLow = premarketLow;
        this.previousClose = previousClose;
        this.marketOpen = marketOpen;
        this.hasBreakout = false;
        this.breakoutDirection = null;
        this.breakoutTime = null;
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getPremarketHigh() {
        return premarketHigh;
    }

    public double getPremarketLow() {
        return premarketLow;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public double getMarketOpen() {
        return marketOpen;
    }

    public boolean isHasBreakout() {
        return hasBreakout;
    }

    public String getBreakoutDirection() {
        return breakoutDirection;
    }

    public String getBreakoutTime() {
        return breakoutTime;
    }

    // Setters
    public void setHasBreakout(boolean hasBreakout) {
        this.hasBreakout = hasBreakout;
    }

    public void setBreakoutDirection(String breakoutDirection) {
        this.breakoutDirection = breakoutDirection;
    }

    public void setBreakoutTime(String breakoutTime) {
        this.breakoutTime = breakoutTime;
    }

    // Helper method to update price and recalculate anything if needed
    public void updatePrice(double newPrice) {
        this.price = newPrice;
    }

    @Override
    public String toString() {
        return "WatchlistItemDTO{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", premarketHigh=" + premarketHigh +
                ", premarketLow=" + premarketLow +
                ", previousClose=" + previousClose +
                ", marketOpen=" + marketOpen +
                ", hasBreakout=" + hasBreakout +
                ", breakoutDirection='" + breakoutDirection + '\'' +
                ", breakoutTime='" + breakoutTime + '\'' +
                '}';
    }
}