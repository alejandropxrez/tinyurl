package distributed.tinyurl.analyticsservice.repository;

import distributed.tinyurl.analyticsservice.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByShortCode(String shortCode);
    long countByShortCode(String shortCode);
}