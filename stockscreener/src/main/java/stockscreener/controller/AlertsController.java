package stockscreener.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import stockscreener.model.PremarketAlert;
import stockscreener.repository.PremarketAlertRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "*")
public class AlertsController {

    private final PremarketAlertRepository alertRepo;

    public AlertsController(PremarketAlertRepository alertRepo) {
        this.alertRepo = alertRepo;
    }

    // GET /alerts → all alerts sorted newest first
    @GetMapping
    public List<PremarketAlert> getAllAlerts() {
        return alertRepo.findAll().stream()
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .toList();
    }

    // GET /alerts/today → only today's alerts sorted newest first
    @GetMapping("/today")
    public List<PremarketAlert> getTodayAlerts() {
        LocalDate today = LocalDate.now();

        return alertRepo.findAll().stream()
                .filter(a -> a.getTriggeredAt().toLocalDate().isEqual(today))
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .toList();
    }
}
