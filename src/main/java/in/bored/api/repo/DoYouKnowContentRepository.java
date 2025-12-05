package in.bored.api.repo;

import in.bored.api.model.DoYouKnowContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoYouKnowContentRepository extends JpaRepository<DoYouKnowContent, Long> {
    @Query(value = """
                SELECT dc.* FROM doyouknow_content dc
                LEFT JOIN user_doyouknow_view udv ON dc.id = udv.doyouknow_content_id AND udv.user_profile_id = :userId
                WHERE udv.id IS NULL
                ORDER BY RANDOM()
                LIMIT :limit
            """, nativeQuery = true)
    List<DoYouKnowContent> findRandomUnseen(@Param("userId") Long userId, @Param("limit") int limit);
}
