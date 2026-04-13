package stockscreener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import stockscreener.service.PriceWebSocketHandler;

@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketConfig.class);

    private final PriceWebSocketHandler priceWebSocketHandler;

    public RawWebSocketConfig(PriceWebSocketHandler priceWebSocketHandler) {
        this.priceWebSocketHandler = priceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(priceWebSocketHandler, "/alerts")
                .setAllowedOriginPatterns("*");  // allow frontend

        log.info("✅ Registered raw WebSocket handler at /alerts");
    }
}
