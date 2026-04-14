package stockscreener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import stockscreener.repository.WatchlistRepository;

import java.time.Instant;
import java.util.List;

@Service
@EnableScheduling
public class AlpacaPricePoller {

    @Autowired
    private WatchlistRepository watchlistRepo;
    
    @Autowired
    private AlpacaDataService alpacaDataService;
    
    @Autowired
    private AlpacaPriceService alpacaPriceService;
    
    @Autowired
    private BreakoutEngineService breakoutEngineService;
    
    @Autowired
    private PremarketLevelsService premarketLevelsService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Poll every 2 seconds
    @Scheduled(fixedDelay = 2000)
    public void pollPrices() {
        List<String> symbols = watchlistRepo.findAllSymbols();
        
        for (String symbol : symbols) {
            try {
                Double price = alpacaDataService.getCurrentPrice(symbol);
                if (price != null && price > 0) {
                    // Update price service
                    alpacaPriceService.updatePrice(symbol, price);
                    
                    // Broadcast to frontend
                    String json = String.format("{\"symbol\":\"%s\",\"price\":%f,\"timestamp\":\"%s\"}", 
                        symbol, price, Instant.now().toString());
                    messagingTemplate.convertAndSend("/topic/prices", json);
                    
                    // Feed to breakout engine
                    breakoutEngineService.onTick(
                        symbol,
                        price,
                        Instant.now(),
                        premarketLevelsService.getLevels(symbol)
                    );
                    
                    System.out.println("📊 POLL: " + symbol + " @ $" + price);
                }
            } catch (Exception e) {
                // Silent fail to avoid spam
            }
        }
    }
}