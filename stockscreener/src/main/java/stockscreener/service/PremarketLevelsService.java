package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.PremarketLevels;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class PremarketLevelsService {

    private final ConcurrentHashMap<String, PremarketLevels> levelsBySymbol =
            new ConcurrentHashMap<>();

    // Store levels for a symbol
    public void setLevels(String symbol, PremarketLevels levels) {
        if (symbol != null && levels != null) {
            levelsBySymbol.put(symbol, levels);
        }
    }

    // Retrieve levels for a symbol
    public PremarketLevels getLevels(String symbol) {
        return levelsBySymbol.get(symbol);
    }

    // Optional: clear all levels (e.g., end of day)
    public void clear() {
        levelsBySymbol.clear();
    }
}
