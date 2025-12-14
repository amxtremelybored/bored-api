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

    @Query(value = """
                SELECT pc.* FROM puzzle_content pc
                LEFT JOIN user_puzzle_view upv ON pc.id = upv.puzzle_content_id AND upv.user_profile_id = :userId
                WHERE upv.id IS NULL
                ORDER BY RANDOM()
                LIMIT 1
            """, nativeQuery = true)
    List<PuzzleContent> findRandomUnseen(@Param("userId") Long userId);

    // Deduplication
    Optional<PuzzleContent> findByQuestion(String question);
}
