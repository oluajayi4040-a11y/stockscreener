package stockscreener.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import stockscreener.model.PremarketAlert;
import stockscreener.model.PremarketLevels;
import stockscreener.repository.PremarketAlertRepository;
import stockscreener.repository.WatchlistRepository;

import java.time.*;
import java.util.List;

@Service
public class PremarketAlertService {

    private final WatchlistRepository watchlistRepo;
    private final PremarketAlertRepository alertRepo;
    private final PolygonService polygonService;
    private final FinnhubPriceService finnhubPriceService;
    private final AlertBroadcastService broadcastService;
    private final EmailService emailService;

    public PremarketAlertService(
            WatchlistRepository watchlistRepo,
            PremarketAlertRepository alertRepo,
            PolygonService polygonService,
            FinnhubPriceService finnhubPriceService,
            AlertBroadcastService broadcastService,
            EmailService emailService
    ) {
        this.watchlistRepo = watchlistRepo;
        this.alertRepo = alertRepo;
        this.polygonService = polygonService;
        this.finnhubPriceService = finnhubPriceService;
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

        // Only between 8:30 AM and 8:40 AM CST
        LocalTime time = now.toLocalTime();
        if (time.isBefore(LocalTime.of(8, 30)) || time.isAfter(LocalTime.of(8, 40))) {
            return;
        }

        // Get all symbols in watchlist
        List<String> symbols = watchlistRepo.findAllSymbols();

        for (String symbol : symbols) {

            // 1. Get premarket high/low from Polygon
            PremarketLevels levels = polygonService.getPremarketLevels(symbol);
            if (levels == null) continue;

            double preHigh = levels.getHigh();
            double preLow  = levels.getLow();

            if (preHigh == 0 || preLow == 0) continue;

            // 2. Get latest real-time price from Finnhub
            double price = finnhubPriceService.getLatestPrice(symbol);
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
                emailService.sendAlertEmail("YOUR_EMAIL@gmail.com", saved);
            }
        }
    }
}
