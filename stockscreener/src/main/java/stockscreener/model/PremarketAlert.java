package stockscreener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PremarketAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String type; // HIGH or LOW

    private double premarketHigh;
    private double premarketLow;

    private double level;   // breakout level (premarket high or low)
    private double price;   // price at trigger

    private LocalDateTime triggeredAt;

    public PremarketAlert() {}

    public PremarketAlert(
            String symbol,
            String type,
            double premarketHigh,
            double premarketLow,
            double level,
            double price
    ) {
        this.symbol = symbol;
        this.type = type;
        this.premarketHigh = premarketHigh;
        this.premarketLow = premarketLow;
        this.level = level;
        this.price = price;
        this.triggeredAt = LocalDateTime.now();
    }

    // ============================
    // REQUIRED GETTERS (Fixes red lines)
    // ============================

    public Long getId() { return id; }

    public String getSymbol() { return symbol; }

    // EmailService expects getAlertType()
    public String getAlertType() { return type; }

    public double getPremarketHigh() { return premarketHigh; }

    public double getPremarketLow() { return premarketLow; }

    // EmailService expects getTriggeredLevel()
    public double getTriggeredLevel() { return level; }

    // EmailService expects getTriggeredPrice()
    public double getTriggeredPrice() { return price; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }

    // ============================
    // Setters (unchanged)
    // ============================

    public void setId(Long id) { this.id = id; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setType(String type) { this.type = type; }
    public void setPremarketHigh(double premarketHigh) { this.premarketHigh = premarketHigh; }
    public void setPremarketLow(double premarketLow) { this.premarketLow = premarketLow; }
    public void setLevel(double level) { this.level = level; }
    public void setPrice(double price) { this.price = price; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
}
