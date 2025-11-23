// src/main/java/in/bored/api/repo/TopicContentRepository.java
package in.bored.api.repo;

import in.bored.api.model.Topic;
import in.bored.api.model.TopicContent;
import in.bored.api.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicContentRepository extends JpaRepository<TopicContent, Long> {

    @Query("""
           select c from TopicContent c
           where c.topic in :topics
             and not exists (
                 select 1 from UserContentView v
                 where v.userProfile = :profile
                   and v.topicContent = c
             )
           order by c.createdAt asc
           """)
    List<TopicContent> findNextUnseenForUser(
            @Param("profile") UserProfile profile,
            @Param("topics") List<Topic> topics,
            Pageable pageable
    );
}