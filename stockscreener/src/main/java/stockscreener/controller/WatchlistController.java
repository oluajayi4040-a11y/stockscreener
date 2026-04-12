package stockscreener.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import stockscreener.dto.WatchlistItemDTO;
import stockscreener.model.Watchlist;
import stockscreener.service.WatchlistService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/watchlist")
@CrossOrigin
public class WatchlistController {

    @Autowired
    private WatchlistService service;

    // ⭐ Return enriched watchlist (previousClose, marketOpen, price)
    @GetMapping
    public List<WatchlistItemDTO> getWatchlist() {
        return service.getWatchlistWithPrices();
    }

    // ⭐ Add symbol
    @PostMapping
    public Watchlist add(@RequestBody Map<String, String> body) {
        return service.addSymbol(body.get("symbol"));
    }

    // ⭐ Delete symbol
    @DeleteMapping("/{symbol}")
    public void delete(@PathVariable String symbol) {
        service.removeSymbol(symbol);
    }
}
