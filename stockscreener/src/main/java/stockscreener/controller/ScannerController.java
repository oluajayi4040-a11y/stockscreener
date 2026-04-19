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
     * Manual trigger for a scan.
     * Replaces old runScan() with the new scan() method.
     */
    @GetMapping("/api/scanner/run")
    public String runScan() {
        scannerService.scan();   // UPDATED — replaces runScan()
        return "Scan triggered";
    }

    /**
     * Simple health check endpoint.
     */
    @GetMapping("/api/scanner/health")
    public String health() {
        return "Scanner is running";
    }
}
