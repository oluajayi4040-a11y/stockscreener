package stockscreener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stockscreener.model.PremarketAlert;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PremarketAlertRepository extends JpaRepository<PremarketAlert, Long> {

    /**
     * Check if an alert already exists for a symbol, alert type, and after a specific time
     * Using 'type' field (not 'alertType')
     */
    @Query("SELECT COUNT(p) > 0 FROM PremarketAlert p WHERE p.symbol = :symbol AND p.type = :type AND p.triggeredAt > :after")
    boolean existsBySymbolAndTypeAndTriggeredAtAfter(
        @Param("symbol") String symbol, 
        @Param("type") String type, 
        @Param("after") LocalDateTime after
    );
    
    /**
     * Delete all alerts for a specific symbol
     */
    void deleteBySymbol(String symbol);
    
    /**
     * Find all alerts for a specific symbol ordered by triggered time
     */
    List<PremarketAlert> findBySymbolOrderByTriggeredAtDesc(String symbol);
    
    /**
     * Find all alerts for today
     */
    @Query("SELECT p FROM PremarketAlert p WHERE DATE(p.triggeredAt) = CURRENT_DATE ORDER BY p.triggeredAt ASC")
    List<PremarketAlert> findTodayAlerts();
    
    /**
     * Count alerts for today grouped by symbol
     */
    @Query("SELECT COUNT(DISTINCT p.symbol) FROM PremarketAlert p WHERE DATE(p.triggeredAt) = CURRENT_DATE")
    long countUniqueSymbolsWithAlertsToday();
    
    /**
     * Find all alerts by type (HIGH or LOW)
     */
    List<PremarketAlert> findByType(String type);
    
    /**
     * Find alerts by symbol and type for today
     */
    @Query("SELECT p FROM PremarketAlert p WHERE p.symbol = :symbol AND p.type = :type AND DATE(p.triggeredAt) = CURRENT_DATE")
    List<PremarketAlert> findTodayAlertsBySymbolAndType(@Param("symbol") String symbol, @Param("type") String type);
}