package stockscreener.dto;

public class QuoteDTO {

    private String symbol;
    private double price;
    private double changePercent;

    public QuoteDTO(String symbol, double price, double changePercent) {
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }
}
