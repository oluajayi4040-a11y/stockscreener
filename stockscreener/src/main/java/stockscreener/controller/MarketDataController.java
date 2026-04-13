package stockscreener.controller;

import org.springframework.web.bind.annotation.*;
import stockscreener.dto.QuoteDTO;
import stockscreener.model.PremarketLevels;
import stockscreener.service.AlpacaDataService;  // ⭐ Changed from PolygonService
import stockscreener.service.FinnhubPriceService;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final AlpacaDataService alpacaDataService;  // ⭐ Changed from PolygonService
    private final FinnhubPriceService finnhubPriceService;

    public MarketDataController(
            AlpacaDataService alpacaDataService,  // ⭐ Changed from PolygonService
            FinnhubPriceService finnhubPriceService
    ) {
        this.alpacaDataService = alpacaDataService;  // ⭐ Changed
        this.finnhubPriceService = finnhubPriceService;
    }

    @GetMapping("/premarket/{symbol}")
    public PremarketLevels getPremarketLevels(@PathVariable String symbol) {
        return alpacaDataService.getPremarketLevels(symbol.toUpperCase());  // ⭐ Changed
    }

    @GetMapping("/price/{symbol}")
    public double getLatestPrice(@PathVariable String symbol) {
        return finnhubPriceService.getLatestPrice(symbol.toUpperCase());
    }

    @GetMapping("/quote/{symbol}")
    public QuoteDTO getCleanQuote(@PathVariable String symbol) {

        String sym = symbol.toUpperCase();
        PremarketLevels levels = alpacaDataService.getPremarketLevels(sym);  // ⭐ Changed
        double price = finnhubPriceService.getLatestPrice(sym);

        return new QuoteDTO(
                sym,
                price,
                levels != null ? levels.getHigh() : 0,
                levels != null ? levels.getLow() : 0
        );
    }
}