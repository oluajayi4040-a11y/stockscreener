package stockscreener.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Watchlist {

    @Id
    private String symbol;

    public Watchlist() {}

    public Watchlist(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
