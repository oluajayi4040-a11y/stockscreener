package stockscreener.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FinnhubPriceService {

    private final ConcurrentHashMap<String, Double> latestPrices = new ConcurrentHashMap<>();

    public void updatePrice(String symbol, double price) {
        latestPrices.put(symbol, price);
    }

    public double getLatestPrice(String symbol) {
        return latestPrices.getOrDefault(symbol, 0.0);
    }
}
