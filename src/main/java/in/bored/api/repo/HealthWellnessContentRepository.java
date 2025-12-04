package in.bored.api.repo;

import in.bored.api.model.HealthWellnessContent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthWellnessContentRepository extends ListCrudRepository<HealthWellnessContent, Long> {

    @Query("""
                SELECT c FROM HealthWellnessContent c
                LEFT JOIN UserHealthWellnessView v ON c.id = v.healthWellnessContentId AND v.userProfileId = :userId
                WHERE c.category.id = :categoryId AND v.id IS NULL
                ORDER BY RANDOM()
                LIMIT 1
            """)
    Optional<HealthWellnessContent> findRandomUnseen(@Param("userId") Long userId,
            @Param("categoryId") UUID categoryId);
}
