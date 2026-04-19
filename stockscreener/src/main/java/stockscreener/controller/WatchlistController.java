package stockscreener.controller;

import org.springframework.web.bind.annotation.*;
import stockscreener.service.WatchlistService;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    /**
     * Get all symbols in the watchlist.
     */
    @GetMapping("/all")
    public List<String> getAll() {
        return watchlistService.getAllSymbols();
    }

    /**
     * Add a symbol to the watchlist.
     */
    @PostMapping("/add/{symbol}")
    public String addSymbol(@PathVariable String symbol) {
        watchlistService.addSymbol(symbol);
        return "Added " + symbol.toUpperCase();
    }

    /**
     * Remove a symbol from the watchlist.
     */
    @DeleteMapping("/remove/{symbol}")
    public String removeSymbol(@PathVariable String symbol) {
        watchlistService.removeSymbol(symbol);
        return "Removed " + symbol.toUpperCase();
    }

    /**
     * Check if a symbol is in the watchlist.
     */
    @GetMapping("/contains/{symbol}")
    public boolean contains(@PathVariable String symbol) {
        return watchlistService.contains(symbol);
    }
}
