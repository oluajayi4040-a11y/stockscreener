package stockscreener.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class ScannerScheduler {

    private final ScannerService scannerService;

    // Market timezone
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    // Scan window: 9:30 AM – 9:50 AM ET
    private static final LocalTime SCAN_START = LocalTime.of(9, 30);
    private static final LocalTime SCAN_END   = LocalTime.of(9, 50);

    public ScannerScheduler(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    /**
     * Runs every 5 seconds.
     * Only triggers scanning during the defined window.
     */
    @Scheduled(fixedRate = 5000)
    public void runScheduledScan() {

        LocalTime now = LocalTime.now(NEW_YORK);

        if (now.isBefore(SCAN_START) || now.isAfter(SCAN_END)) {
            return; // outside scan window
        }

        try {
            System.out.println("⏱ Running scheduled scan at " + now);
            scannerService.scan();   // NEW — replaces runScan()
        } catch (Exception e) {
            System.out.println("❌ ScannerScheduler error: " + e.getMessage());
        }
    }

    /**
     * Daily reset at 10 PM CST.
     * This clears any in‑memory state in your breakout engine (if needed).
     */
    @Scheduled(cron = "0 0 22 * * MON-FRI", zone = "America/Chicago")
    public void resetDaily() {
        System.out.println("🔄 Daily reset at 10 PM CST...");
        // No resetTriggeredToday() anymore — handled inside engine if needed
    }
}
