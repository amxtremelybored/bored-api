package in.bored.api.repo;

import in.bored.api.model.QuizContent;
import in.bored.api.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizContentRepository extends JpaRepository<QuizContent, Long> {

    @Query(value = "SELECT * FROM quiz_content q WHERE q.id NOT IN (SELECT v.quiz_content_id FROM user_quiz_view v WHERE v.user_profile_id = :userProfileId) ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    java.util.List<QuizContent> findRandomUnseen(@Param("userProfileId") Long userProfileId, @Param("limit") int limit);

    // Deduplication
    Optional<QuizContent> findByQuestion(String question);
}
