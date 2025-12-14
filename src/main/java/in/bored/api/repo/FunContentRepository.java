package in.bored.api.repo;

import in.bored.api.model.FunContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FunContentRepository extends JpaRepository<FunContent, Long> {
    @Query(value = """
                SELECT fc.* FROM fun_content fc
                LEFT JOIN user_fun_view ufv ON fc.id = ufv.fun_content_id AND ufv.user_profile_id = :userId
                WHERE ufv.id IS NULL
                ORDER BY RANDOM()
                LIMIT :limit
            """, nativeQuery = true)
    List<FunContent> findRandomUnseen(@Param("userId") Long userId, @Param("limit") int limit);

    // Deduplication
    java.util.Optional<FunContent> findByContent(String content);
}
