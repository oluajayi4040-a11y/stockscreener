package stockscreener.breakout.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that exposes the institutional SignalEngine
 * as a Spring-managed bean.
 *
 * This allows the engine to be injected into ScannerService, SignalDetector,
 * or any other service that needs breakout evaluation.
 */
@Configuration
public class SignalEngineConfig {

    /**
     * Creates a singleton SignalEngine bean.
     */
    @Bean
    public SignalEngine signalEngine() {
        return new SignalEngine();
    }
}
