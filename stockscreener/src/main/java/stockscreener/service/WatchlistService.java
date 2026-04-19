package stockscreener.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WatchlistService {

    // In-memory storage for now (can be replaced with DB later)
    private final Set<String> symbols = Collections.synchronizedSet(new HashSet<>());

    public WatchlistService() {
        // Default starter list — you can remove or expand this
        symbols.add("AAPL");
        symbols.add("TSLA");
        symbols.add("NVDA");
        symbols.add("META");
        symbols.add("AMD");
    }

    /**
     * Add a symbol to the watchlist.
     */
    public void addSymbol(String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            symbols.add(symbol.toUpperCase());
        }
    }

    /**
     * Remove a symbol from the watchlist.
     */
    public void removeSymbol(String symbol) {
        if (symbol != null) {
            symbols.remove(symbol.toUpperCase());
        }
    }

    /**
     * Return all symbols in the watchlist.
     */
    public List<String> getAllSymbols() {
        return symbols.stream().sorted().toList();
    }

    /**
     * Check if a symbol is in the watchlist.
     */
    public boolean contains(String symbol) {
        return symbols.contains(symbol.toUpperCase());
    }
}
