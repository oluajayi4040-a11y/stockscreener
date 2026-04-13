package stockscreener.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
public class Watchlist {

    @Id
    private String symbol;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    public Watchlist() {}

    public Watchlist(String symbol) {
        this.symbol = symbol;
        this.addedAt = LocalDateTime.now();
    }

    public Watchlist(String symbol, String companyName) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.addedAt = LocalDateTime.now();
    }

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

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}