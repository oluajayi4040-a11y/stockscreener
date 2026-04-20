package stockscreener.service;

import stockscreener.model.PremarketLevels;
import stockscreener.model.MinuteBar;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Unified market data abstraction for Alpaca, Polygon, or any other provider.
 *
 * This interface supports:
 *  - Original methods (unchanged)
 *  - Institutional breakout engine methods:
 *        getLatestMinuteBar()
 *        getVWAP()
 *        getAverage1MinVolume()
 */
public interface MarketDataClient {

    // -----------------------------
    // ORIGINAL METHODS (UNCHANGED)
    // -----------------------------

    /**
     * Returns the last traded price for the symbol.
     */
    Double getLastPrice(String symbol);

    /**
     * Returns the previous day's closing price.
     */
    Double getPreviousClose(String symbol);

    /**
     * Returns premarket high, low, volume, and previous close.
     */
    PremarketLevels getPremarketLevels(String symbol);

    /**
     * Returns total premarket volume.
     */
    Double getPremarketVolume(String symbol);

    /**
     * Returns current day's total volume.
     */
    Double getCurrentVolume(String symbol);

    /**
     * Returns average daily volume (ADV).
     */
    Double getAverageVolume(String symbol);

    /**
     * Returns total options volume for the day.
     */
    Double getOptionsVolume(String symbol);

    /**
     * Returns the company name.
     */
    String getCompanyName(String symbol);

    /**
     * Returns historical minute bars for a time range.
     */
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
