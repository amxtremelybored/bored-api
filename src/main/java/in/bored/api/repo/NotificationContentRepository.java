package in.bored.api.repo;

import in.bored.api.model.NotificationContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationContentRepository extends JpaRepository<NotificationContent, Long> {

    @Query(value = "SELECT * FROM notification_content WHERE viewed = false ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<NotificationContent> findRandomUnviewed();
}
