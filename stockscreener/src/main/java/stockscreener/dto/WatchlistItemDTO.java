package stockscreener.dto;

public class WatchlistItemDTO {

    private String symbol;
    private double previousClose;
    private double marketOpen;
    private double price;

    // ⭐ NEW FIELDS
    private double premarketHigh;
    private double premarketLow;

    public WatchlistItemDTO(
            String symbol,
            double previousClose,
            double marketOpen,
            double price,
            double premarketHigh,
            double premarketLow
    ) {
        this.symbol = symbol;
        this.previousClose = previousClose;
        this.marketOpen = marketOpen;
        this.price = price;
        this.premarketHigh = premarketHigh;
        this.premarketLow = premarketLow;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public double getMarketOpen() {
        return marketOpen;
    }

    public double getPrice() {
        return price;
    }

    // ⭐ NEW GETTERS
    public double getPremarketHigh() {
        return premarketHigh;
    }

    public double getPremarketLow() {
        return premarketLow;
    }
}
