package stockscreener.service;

import org.springframework.stereotype.Service;
import stockscreener.model.MinuteBar;

import java.util.List;

@Service
public class VWAPCalculator {

    /**
     * Computes VWAP from a list of MinuteBar objects.
     */
    public double computeVWAP(List<MinuteBar> bars) {

        if (bars == null || bars.isEmpty()) {
            return 0.0;
        }

        double cumulativePV = 0.0;
        double cumulativeVolume = 0.0;

        for (MinuteBar bar : bars) {
            double typicalPrice = (bar.getHigh() + bar.getLow() + bar.getClose()) / 3.0;
            double volume = bar.getVolume();

            cumulativePV += typicalPrice * volume;
            cumulativeVolume += volume;
        }

        if (cumulativeVolume == 0) {
            return 0.0;
        }

        return cumulativePV / cumulativeVolume;
    }
}
