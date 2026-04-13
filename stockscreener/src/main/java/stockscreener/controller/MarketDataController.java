package stockscreener.controller;

import org.springframework.web.bind.annotation.*;
import stockscreener.dto.QuoteDTO;
import stockscreener.model.PremarketLevels;
import stockscreener.service.PolygonService;
import stockscreener.service.FinnhubPriceService;

@RestController
@RequestMapping("/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final PolygonService polygonService;
    private final FinnhubPriceService finnhubPriceService;

    public MarketDataController(
            PolygonService polygonService,
            FinnhubPriceService finnhubPriceService
    ) {
        this.polygonService = polygonService;
        this.finnhubPriceService = finnhubPriceService;
    }

    @GetMapping("/premarket/{symbol}")
    public PremarketLevels getPremarketLevels(@PathVariable String symbol) {
        return polygonService.getPremarketLevels(symbol.toUpperCase());
    }

    @GetMapping("/price/{symbol}")
    public double getLatestPrice(@PathVariable String symbol) {
        return finnhubPriceService.getLatestPrice(symbol.toUpperCase());
    }

    @GetMapping("/quote/{symbol}")
    public QuoteDTO getCleanQuote(@PathVariable String symbol) {

        String sym = symbol.toUpperCase();
        PremarketLevels levels = polygonService.getPremarketLevels(sym);
        double price = finnhubPriceService.getLatestPrice(sym);

        return new QuoteDTO(
                sym,
                price,
                levels != null ? levels.getHigh() : 0,
                levels != null ? levels.getLow() : 0
        );
    }
}
