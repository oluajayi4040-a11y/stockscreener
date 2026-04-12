package stockscreener.controller;

import stockscreener.service.AlpacaService;
import stockscreener.dto.QuoteDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final AlpacaService alpacaService;

    public MarketDataController(AlpacaService alpacaService) {
        this.alpacaService = alpacaService;
    }

    // ORIGINAL ENDPOINT (unchanged)
    @GetMapping("/quote/raw/{symbol}")
    public String getRawQuote(@PathVariable String symbol) {
        return alpacaService.getLatestQuote(symbol.toUpperCase());
    }

    // NEW CLEAN ENDPOINT (returns QuoteDTO)
    @GetMapping("/quote/{symbol}")
    public QuoteDTO getCleanQuote(@PathVariable String symbol) {
        return alpacaService.getCleanQuote(symbol.toUpperCase());
    }
}
