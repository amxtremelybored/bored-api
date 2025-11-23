// src/main/java/in/bored/api/repo/TopicContentRepository.java
package in.bored.api.repo;

import in.bored.api.model.Topic;
import in.bored.api.model.TopicContent;
import in.bored.api.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicContentRepository extends JpaRepository<TopicContent, Long> {

    @Query("""
           SELECT tc
           FROM TopicContent tc
           WHERE tc.topic IN :topics
             AND NOT EXISTS (
                 SELECT 1
                 FROM UserContentView v
                 WHERE v.userProfile = :profile
                   AND v.topicContent = tc
           )
           ORDER BY tc.id ASC
           """)
    List<TopicContent> findNextUnseenForUser(UserProfile profile,
                                             List<Topic> topics,
                                             Pageable pageable);
}