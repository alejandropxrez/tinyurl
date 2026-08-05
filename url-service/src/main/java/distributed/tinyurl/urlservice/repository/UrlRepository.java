package distributed.tinyurl.urlservice.repository;

import distributed.tinyurl.urlservice.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);
    Optional<Url> findByShortCodeAndUserId(String shortCode, Long userId);
    List<Url> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByShortCode(String shortCode);
}
