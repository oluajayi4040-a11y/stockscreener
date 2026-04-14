package stockscreener.controller;

import org.springframework.web.bind.annotation.*;
import stockscreener.dto.QuoteDTO;
import stockscreener.model.PremarketLevels;
import stockscreener.service.AlpacaDataService;
import stockscreener.service.AlpacaPriceService;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final AlpacaDataService alpacaDataService;
    private final AlpacaPriceService alpacaPriceService;

    public MarketDataController(
            AlpacaDataService alpacaDataService,
            AlpacaPriceService alpacaPriceService
    ) {
        this.alpacaDataService = alpacaDataService;
        this.alpacaPriceService = alpacaPriceService;
    }

    @GetMapping("/premarket/{symbol}")
    public PremarketLevels getPremarketLevels(@PathVariable String symbol) {
        return alpacaDataService.getPremarketLevels(symbol.toUpperCase());
    }

    @GetMapping("/price/{symbol}")
    public double getLatestPrice(@PathVariable String symbol) {
        return alpacaPriceService.getLatestPrice(symbol.toUpperCase());
    }

    @GetMapping("/quote/{symbol}")
    public QuoteDTO getCleanQuote(@PathVariable String symbol) {

        String sym = symbol.toUpperCase();
        PremarketLevels levels = alpacaDataService.getPremarketLevels(sym);
        double price = alpacaPriceService.getLatestPrice(sym);

        return new QuoteDTO(
                sym,
                price,
                levels != null ? levels.getHigh() : 0,
                levels != null ? levels.getLow() : 0
        );
    }
}