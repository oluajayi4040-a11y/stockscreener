package stockscreener.dto;

public class QuoteDTO {

    private String symbol;
    private double price;
    private double premarketHigh;
    private double premarketLow;

    public QuoteDTO(String symbol, double price, double premarketHigh, double premarketLow) {
        this.symbol = symbol;
        this.price = price;
        this.premarketHigh = premarketHigh;
        this.premarketLow = premarketLow;
    }

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

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setPremarketHigh(double premarketHigh) {
        this.premarketHigh = premarketHigh;
    }

    public void setPremarketLow(double premarketLow) {
        this.premarketLow = premarketLow;
    }
}
