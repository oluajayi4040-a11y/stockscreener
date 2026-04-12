package stockscreener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stockscreener.model.PremarketAlert;

public interface PremarketAlertRepository extends JpaRepository<PremarketAlert, Long> {
}
