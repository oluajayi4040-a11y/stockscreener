package stockscreener.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stockscreener.model.PremarketAlert;
import stockscreener.model.PremarketLevels;
import stockscreener.repository.PremarketAlertRepository;
import stockscreener.repository.WatchlistRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PremarketAlertService {

    private final WatchlistRepository watchlistRepo;
    private final PremarketAlertRepository alertRepo;
    private final AlpacaDataService alpacaDataService;
    private final AlpacaPriceService alpacaPriceService;
    private final AlertBroadcastService broadcastService;
    private final EmailService emailService;

    public PremarketAlertService(
            WatchlistRepository watchlistRepo,
            PremarketAlertRepository alertRepo,
            AlpacaDataService alpacaDataService,
            AlpacaPriceService alpacaPriceService,
            AlertBroadcastService broadcastService,
            EmailService emailService
    ) {
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.alpacaDataService = alpacaDataService;
        this.alpacaPriceService = alpacaPriceService;
        this.broadcastService = broadcastService;
        this.emailService = emailService;
    }

    // Runs every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void checkPremarketBreakouts() {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Chicago"));

        // Only Monday–Friday
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return;
        }

        // TEST MODE: Expanded window from 8:30 AM to 3:00 PM CST (9:30 AM - 4:00 PM ET)
        LocalTime time = now.toLocalTime();
        LocalTime startTime = LocalTime.of(8, 30);  // 8:30 AM CST
        LocalTime endTime = LocalTime.of(15, 0);     // 3:00 PM CST
        
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return;
        }

        // Get all symbols in watchlist
        List<String> symbols = watchlistRepo.findAllSymbols();
        
        // Get start of today for duplicate checking
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        for (String symbol : symbols) {

            // 1. Get premarket high/low from Alpaca
            PremarketLevels levels = alpacaDataService.getPremarketLevels(symbol);
            if (levels == null) continue;

            double preHigh = levels.getHigh();
            double preLow  = levels.getLow();

            if (preHigh == 0 || preLow == 0) continue;

            // 2. Get latest real-time price from Alpaca Price Service
            double price = alpacaPriceService.getLatestPrice(symbol);
            if (price <= 0) continue;

            // 3. Check HIGH breakout
            if (price > preHigh) {
                // ⭐ Fixed: Using correct method name 'existsBySymbolAndTypeAndTriggeredAtAfter'
                boolean alreadyAlerted = alertRepo.existsBySymbolAndTypeAndTriggeredAtAfter(
                    symbol, "HIGH", startOfDay
                );
                
                if (!alreadyAlerted) {
                    System.out.println("🚨 NEW HIGH BREAKOUT: " + symbol + " at $" + price);
                    
                    PremarketAlert alert = new PremarketAlert(
                            symbol,
                            "HIGH",
                            preHigh,
                            preLow,
                            preHigh,
                            price
                    );

                    PremarketAlert saved = alertRepo.save(alert);
                    broadcastService.sendAlert(saved);
                    emailService.sendAlertEmail("YOUR_EMAIL@gmail.com", saved);
                    System.out.println("📧 Email alert sent for " + symbol + " HIGH breakout at $" + price);
                }
            }

            // 4. Check LOW breakout
            if (price < preLow) {
                // ⭐ Fixed: Using correct method name 'existsBySymbolAndTypeAndTriggeredAtAfter'
                boolean alreadyAlerted = alertRepo.existsBySymbolAndTypeAndTriggeredAtAfter(
                    symbol, "LOW", startOfDay
                );
                
                if (!alreadyAlerted) {
                    System.out.println("🚨 NEW LOW BREAKOUT: " + symbol + " at $" + price);
                    
                    PremarketAlert alert = new PremarketAlert(
                            symbol,
                            "LOW",
                            preHigh,
                            preLow,
                            preLow,
                            price
                    );

                    PremarketAlert saved = alertRepo.save(alert);
                    broadcastService.sendAlert(saved);
                    emailService.sendAlertEmail("YOUR_EMAIL@gmail.com", saved);
                    System.out.println("📧 Email alert sent for " + symbol + " LOW breakout at $" + price);
                }
            }
        }
    }
    
    /**
     * Manually clear all alerts for a symbol (useful when removing from watchlist)
     */
    @Transactional
    public void clearAlertsForSymbol(String symbol) {
        alertRepo.deleteBySymbol(symbol);
        System.out.println("🗑️ Cleared all alerts for " + symbol);
    }
}