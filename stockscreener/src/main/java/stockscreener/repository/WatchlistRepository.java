package stockscreener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stockscreener.model.Watchlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    /**
     * Find a watchlist entry by symbol.
     */
    Optional<Watchlist> findBySymbol(String symbol);

    /**
     * Delete a symbol from the watchlist.
     */
    void deleteBySymbol(String symbol);

    /**
     * Return all symbols in the watchlist as a list of strings.
     */
    List<Watchlist> findAll();
}
