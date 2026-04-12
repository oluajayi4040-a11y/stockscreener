package stockscreener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import stockscreener.dto.WatchlistItemDTO;
import stockscreener.model.Watchlist;
import stockscreener.repository.WatchlistRepository;

// ⭐ ADD THIS IMPORT
import stockscreener.service.PremarketLevels;

import java.util.ArrayList;
import java.util.List;

@Service
public class WatchlistService {

    @Autowired
    private WatchlistRepository repo;

    @Autowired
    private AlpacaWebSocketClient alpacaWebSocketClient;

    @Autowired
    private AlpacaService alpacaService;

    // ⭐ Return full watchlist with previousClose, marketOpen, latest price, and premarket levels
    public List<WatchlistItemDTO> getWatchlistWithPrices() {
        List<String> symbols = repo.findAllSymbols();
        List<WatchlistItemDTO> result = new ArrayList<>();

        for (String symbol : symbols) {

            // Previous close
            double previousClose = alpacaService.getPreviousClose(symbol);

            // Market open
            double marketOpen = alpacaService.getMarketOpen(symbol);

            // Latest real-time price (WebSocket first, REST fallback)
            double latestPrice = alpacaService.getLatestPrice(symbol);

            // ⭐ NEW — Premarket high/low
            PremarketLevels levels = alpacaService.getPremarketLevels(symbol);
            double preHigh = (levels != null) ? levels.getHigh() : 0.0;
            double preLow  = (levels != null) ? levels.getLow()  : 0.0;

            // Build DTO
            result.add(new WatchlistItemDTO(
                    symbol,
                    previousClose,
                    marketOpen,
                    latestPrice,
                    preHigh,
                    preLow
            ));
        }

        return result;
    }

    // ⭐ Add symbol + auto-refresh WebSocket subscriptions
    public Watchlist addSymbol(String symbol) {
        Watchlist item = new Watchlist(symbol.toUpperCase());
        Watchlist saved = repo.save(item);

        alpacaWebSocketClient.refreshSubscriptions();
        return saved;
    }

    // ⭐ Remove symbol + auto-refresh WebSocket subscriptions
    public void removeSymbol(String symbol) {
        repo.deleteBySymbol(symbol.toUpperCase());
        alpacaWebSocketClient.refreshSubscriptions();
    }
}
