package stockscreener.service;

import stockscreener.model.PremarketLevels;
import stockscreener.model.MinuteBar;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Unified market data abstraction for Alpaca, Polygon, or any other provider.
 *
 * This interface now supports:
 *  - Your original methods (unchanged)
 *  - New institutional breakout engine methods:
 *        getLatestMinuteBar()
 *        getVWAP()
 *        getAverage1MinVolume()
 */
public interface MarketDataClient {

    // -----------------------------
    // ORIGINAL METHODS (UNCHANGED)
    // -----------------------------

    double getLastPrice(String symbol);

    Double getPreviousClose(String symbol);

    PremarketLevels getPremarketLevels(String symbol);

    double getPremarketVolume(String symbol);

    double getCurrentVolume(String symbol);

    Double getAverageVolume(String symbol);

    Double getOptionsVolume(String symbol);

    String getCompanyName(String symbol);

    List<MinuteBar> getMinuteBars(String symbol, ZonedDateTime start, ZonedDateTime end);


    // -----------------------------------------
    // NEW METHODS REQUIRED BY SIGNAL ENGINE
    // -----------------------------------------

    /**
     * Returns the latest 1-minute bar for the symbol.
     */
    MinuteBar getLatestMinuteBar(String symbol);

    /**
     * Returns the current VWAP for the symbol.
     */
    Double getVWAP(String symbol);

    /**
     * Returns the average 1-minute volume for the symbol.
     */
    Long getAverage1MinVolume(String symbol);
}
