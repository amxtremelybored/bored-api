// src/main/java/in/bored/api/repo/TopicRepository.java
package in.bored.api.repo;

import in.bored.api.model.ContentCategory;
import in.bored.api.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCategoryInAndContentLoadedTrue(List<ContentCategory> categories);

    List<Topic> findByCategoryIn(List<ContentCategory> categories);

    List<Topic> findAllByContentLoadedTrue();

    @Query(value = "SELECT * FROM topics WHERE is_content_loaded = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Topic findRandomTopic();

    @Query(value = "SELECT t.* FROM topics t JOIN topic_contents tc ON t.id = tc.topic_id WHERE t.is_content_loaded = true ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Topic findRandomTopicWithContent();

    List<Topic> findByNameContainingIgnoreCase(String name);
}