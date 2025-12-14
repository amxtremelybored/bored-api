package in.bored.api.repo;

import in.bored.api.model.PuzzleContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PuzzleContentRepository extends JpaRepository<PuzzleContent, Long> {

    @Query(value = "SELECT * FROM puzzle_content q WHERE q.id NOT IN (SELECT v.puzzle_content_id FROM user_puzzle_view v WHERE v.user_profile_id = :userId) ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<PuzzleContent> findRandomUnseen(@Param("userId") Long userId, @Param("limit") int limit);

    // Deduplication
    Optional<PuzzleContent> findByQuestion(String question);
}
