package stockscreener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import stockscreener.model.Watchlist;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    @Query("SELECT w.symbol FROM Watchlist w")
    List<String> findAllSymbols();

    void deleteBySymbol(String symbol);
}
