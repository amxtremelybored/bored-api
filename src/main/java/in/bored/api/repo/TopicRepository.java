// src/main/java/in/bored/api/repo/TopicRepository.java
package in.bored.api.repo;

import in.bored.api.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}