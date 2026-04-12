package stockscreener.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketAlert;
import stockscreener.repository.PremarketAlertRepository;
import stockscreener.repository.WatchlistRepository;

import java.time.*;
import java.util.List;

@Service
public class PremarketAlertService {

    private final WatchlistRepository watchlistRepo;
    private final PremarketAlertRepository alertRepo;
    private final AlpacaService alpacaService;
    private final AlertBroadcastService broadcastService;
    private final EmailService emailService;   // ⭐ NEW

    public PremarketAlertService(
            WatchlistRepository watchlistRepo,
            PremarketAlertRepository alertRepo,
            AlpacaService alpacaService,
            AlertBroadcastService broadcastService,
            EmailService emailService   // ⭐ NEW
    ) {
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.alpacaService = alpacaService;
        this.broadcastService = broadcastService;
        this.emailService = emailService;   // ⭐ NEW
    }

    // Runs every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void checkPremarketBreakouts() {

        // ⭐ Run only between 8:29 AM and 8:35 AM CST
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Chicago"));
        LocalTime time = now.toLocalTime();

        if (time.isBefore(LocalTime.of(8, 29)) || time.isAfter(LocalTime.of(8, 35))) {
            return;
        }

        // Get all symbols in watchlist
        List<String> symbols = watchlistRepo.findAllSymbols();

        for (String symbol : symbols) {

            // 1. Get premarket high/low
            var levels = alpacaService.getPremarketLevels(symbol);
            if (levels == null) continue;

            // ⭐ UPDATED — use getters
            double preHigh = levels.getHigh();
            double preLow  = levels.getLow();

            // 2. Get latest price
            double price = alpacaService.getLatestPrice(symbol);
            if (price <= 0) continue;

            // 3. Check breakout
            if (price > preHigh) {
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

                // ⭐ Send Email Alert
                emailService.sendAlertEmail("YOUR_EMAIL@gmail.com", saved);
            }

            if (price < preLow) {
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

                // ⭐ Send Email Alert
                emailService.sendAlertEmail("YOUR_EMAIL@gmail.com", saved);
            }
        }
    }
}
