package in.bored.api.repo;

import in.bored.api.model.JokeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JokeContentRepository extends JpaRepository<JokeContent, Long> {
    @Query(value = """
                SELECT jc.* FROM joke_content jc
                LEFT JOIN user_joke_view ujv ON jc.id = ujv.joke_content_id AND ujv.user_profile_id = :userId
                WHERE ujv.id IS NULL
                ORDER BY RANDOM()
                LIMIT :limit
            """, nativeQuery = true)
    List<JokeContent> findRandomUnseen(@Param("userId") Long userId, @Param("limit") int limit);
}
