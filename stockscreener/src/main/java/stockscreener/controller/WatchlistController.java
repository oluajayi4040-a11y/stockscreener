package stockscreener.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import stockscreener.dto.WatchlistItemDTO;
import stockscreener.model.Watchlist;
import stockscreener.service.WatchlistService;
import stockscreener.service.FinnhubWebSocketClient;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistController {

    @Autowired
    private WatchlistService service;

    @Autowired
    private FinnhubWebSocketClient finnhubWebSocketClient;

    // ⭐ Return watchlist with real-time price + premarket levels
    @GetMapping
    public List<WatchlistItemDTO> getWatchlist() {
        return service.getWatchlistWithPrices();
    }

    // ⭐ Add symbol (POST body: { "symbol": "AAPL" })
    @PostMapping
    public Watchlist add(@RequestBody Map<String, String> body) {
        return service.addSymbol(body.get("symbol"));
    }

    // ⭐ Delete symbol with error handling
    @DeleteMapping("/{symbol}")
    public ResponseEntity<?> delete(@PathVariable String symbol) {
        try {
            service.removeSymbol(symbol);
            return ResponseEntity.ok().body("Symbol " + symbol + " removed successfully");
        } catch (Exception e) {
            System.err.println("Error deleting symbol " + symbol + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting symbol: " + e.getMessage());
        }
    }

    // ⭐ Resubscribe a single symbol to WebSocket
    @PostMapping("/resubscribe/{symbol}")
    public ResponseEntity<String> resubscribe(@PathVariable String symbol) {
        try {
            String sym = symbol.toUpperCase();
            finnhubWebSocketClient.subscribe(sym);
            return ResponseEntity.ok("✅ Resubscribed to " + sym);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to resubscribe: " + e.getMessage());
        }
    }

    // ⭐ Resubscribe all symbols to WebSocket
    @PostMapping("/resubscribe-all")
    public ResponseEntity<String> resubscribeAll() {
        try {
            List<WatchlistItemDTO> watchlist = service.getWatchlistWithPrices();
            int count = 0;
            for (WatchlistItemDTO item : watchlist) {
                finnhubWebSocketClient.subscribe(item.getSymbol());
                count++;
                Thread.sleep(100);
            }
            return ResponseEntity.ok("✅ Resubscribed to " + count + " symbols");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to resubscribe: " + e.getMessage());
        }
    }

    // ⭐ NEW: Health check endpoint to see price update status
    @GetMapping("/health/prices")
    public Map<String, Object> checkPriceHealth() {
        Map<String, Object> status = new HashMap<>();
        List<WatchlistItemDTO> watchlist = service.getWatchlistWithPrices();
        
        long symbolsWithPrices = watchlist.stream().filter(w -> w.getPrice() > 0).count();
        long totalSymbols = watchlist.size();
        
        // Get symbols without prices for debugging
        List<String> symbolsWithoutPrices = watchlist.stream()
                .filter(w -> w.getPrice() == 0)
                .map(WatchlistItemDTO::getSymbol)
                .toList();
        
        status.put("totalSymbols", totalSymbols);
        status.put("symbolsWithPrices", symbolsWithPrices);
        status.put("symbolsWithoutPrices", symbolsWithoutPrices.size());
        status.put("percentage", (symbolsWithPrices * 100 / totalSymbols) + "%");
        status.put("symbolsWithoutPricesList", symbolsWithoutPrices);
        status.put("timestamp", new Date());
        
        return status;
    }

    // ⭐ NEW: Get specific symbol details for debugging
    @GetMapping("/debug/{symbol}")
    public Map<String, Object> debugSymbol(@PathVariable String symbol) {
        Map<String, Object> debug = new HashMap<>();
        List<WatchlistItemDTO> watchlist = service.getWatchlistWithPrices();
        
        WatchlistItemDTO symbolData = watchlist.stream()
                .filter(w -> w.getSymbol().equalsIgnoreCase(symbol))
                .findFirst()
                .orElse(null);
        
        if (symbolData != null) {
            debug.put("symbol", symbolData.getSymbol());
            debug.put("price", symbolData.getPrice());
            debug.put("companyName", symbolData.getCompanyName());
            debug.put("premarketHigh", symbolData.getPremarketHigh());
            debug.put("premarketLow", symbolData.getPremarketLow());
            debug.put("previousClose", symbolData.getPreviousClose());
            debug.put("marketOpen", symbolData.getMarketOpen());
            debug.put("hasBreakout", symbolData.isHasBreakout());
            debug.put("breakoutDirection", symbolData.getBreakoutDirection());
            debug.put("breakoutTime", symbolData.getBreakoutTime());
        } else {
            debug.put("error", "Symbol not found in watchlist");
        }
        
        return debug;
    }
}