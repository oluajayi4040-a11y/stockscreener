package stockscreener.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import stockscreener.service.ScannerService;

@RestController
public class ScannerController {

    private final ScannerService scannerService;

    public ScannerController(ScannerService scannerService) {
        this.scannerService = scannerService;
    }

    /**
     * Manual trigger for a full universe scan.
     * Uses the new dynamic S&P 500 universe + institutional filters.
     */
    @GetMapping("/api/scanner/run")
    public String runScan() {
        scannerService.scan();
        return "Universe scan triggered";
    }

    /**
     * Simple health check endpoint.
     */
    @GetMapping("/api/scanner/health")
    public String health() {
        return "Scanner is running";
    }
}
